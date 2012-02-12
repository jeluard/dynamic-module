package org.mule.tools.module.loader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.mule.tools.module.helper.Classes;
import org.mule.tools.module.helper.Jars;
import org.mule.tools.module.helper.Modules;
import org.mule.tools.module.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarLoader extends Loader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JarLoader.class.getPackage().getName());

    private static final String MODULE_CLASS_SUFFIX = "Module";
    private static final String CONNECTOR_CLASS_SUFFIX = "Connector";
    private static final String CONNECTION_MANAGER_CLASS_SUFFIX = "ConnectionManager";

    /**
     * @param fileNames
     * @return all potential {@link Module} class name among specified `fileNames`
     */
    protected final List<String> findPotentialModuleClassNames(final List<String> fileNames) {
        final List<String> potentialModuleClassNames = new LinkedList<String>();
        for (final String fileName : fileNames) {
            if (fileName.endsWith(JarLoader.MODULE_CLASS_SUFFIX+".class") ||
                    fileName.endsWith(JarLoader.CONNECTOR_CLASS_SUFFIX+".class")) {
                potentialModuleClassNames.add(fileName);
            }
        }
        return potentialModuleClassNames;
    }

    protected final boolean isValidModuleClass(final Class<?> clazz) {
        final Annotation[] annotations = clazz.getAnnotations();
        final String moduleAnnotationClassName = "org.mule.api.annotations.Module";
        final String connectorAnnotationClassName = "org.mule.api.annotations.Connector";
        for (final Annotation annotation : annotations) {
            if (moduleAnnotationClassName.equals(annotation.annotationType().getName()) || 
                connectorAnnotationClassName.equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param fileNames
     * @param classLoader
     * @return first module found among `fileNames`
     */
    protected final Class<?> findModuleClass(final List<String> potentialModuleClassNames, final ClassLoader classLoader) {
        for (final String potentialModuleClassName : potentialModuleClassNames) {
            final String className = extractClassName(potentialModuleClassName);
            final Class<?> moduleClass = Classes.loadClass(classLoader, className);
            if (moduleClass == null) {
                throw new IllegalArgumentException("Failed to load <"+className+">");
            }

            if (!isValidModuleClass(moduleClass)) {
                if (JarLoader.LOGGER.isWarnEnabled()) {
                    JarLoader.LOGGER.warn("Skipping invalid module <"+className+">");
                }

                continue;
            }

            return moduleClass;
        }
        return null;
    }

    /**
     * @param moduleSubClasses
     * @return {@link Class} among specified classes having biggest number of parent {@link Class}es
     */
    protected final Class<?> findMostSpecificSubClass(final List<Class<?>> moduleSubClasses) {
        return Collections.max(moduleSubClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(final Class<?> class1, final Class<?> class2) {
                return Integer.valueOf(Classes.allSuperClasses(class1).size()).compareTo(Classes.allSuperClasses(class2).size());
            }
        });
    }

    /**
     * @param generatedPackageName
     * @param moduleName
     * @return {@link ConnectionManager} Class name if any, null otherwise
     */
    protected final String extractConnectionManagerClassName(final String generatedPackageName, final String moduleName, final Object module) {
        if (Modules.isConnectionManagementCapable(module)) {
            return generatedPackageName+"."+moduleName+JarLoader.CONNECTION_MANAGER_CLASS_SUFFIX;
        }
        return null;
    }

    /**
     * @param moduleClass
     * @param fileNames
     * @param classLoader
     * @return all {@link Module} sub {@link Class}es
     */
    protected final List<Class<?>> findModuleSubClasses(final Class<?> moduleClass, final List<String> fileNames, final ClassLoader classLoader) {
        final String moduleClassSimpleName = moduleClass.getSimpleName();
        final List<Class<?>> subClasses = new LinkedList<Class<?>>();
        for (final String fileName : fileNames) {
            if (fileName.contains(moduleClassSimpleName)) {
                final String className = extractClassName(fileName);
                try {
                    final Class<?> clazz = Classes.loadClass(classLoader, className);
                    //Ensures this is effectively a module subclass
                    if (Classes.allSuperClasses(clazz).contains(moduleClass)) {
                        subClasses.add(clazz);
                    }
                } catch (Error e) {
                    if (JarLoader.LOGGER.isWarnEnabled()) {
                        JarLoader.LOGGER.warn("Failed to load <"+className+">", e);
                    }
                }
            }
        }
        return subClasses;
    }

    /**
     * @param urls
     * @return a {@link Module} representation of first module found in specified `urls`
     * @throws IOException 
     */
    public final Module load(final List<URL> urls) throws IOException {
        final URL moduleJar = urls.get(0);
        final List<String> allFileNames = Jars.allFileNames(moduleJar);
        final ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            final List<String> potentialModuleClassNames = findPotentialModuleClassNames(allFileNames);
            if (potentialModuleClassNames.isEmpty()) {
                throw new IllegalArgumentException("Failed to find potential Module class among <"+allFileNames+">");
            }
            final Class<?> moduleClass = findModuleClass(potentialModuleClassNames, classLoader);
            if (moduleClass == null) {
                throw new IllegalArgumentException("Failed to find Module class in <"+potentialModuleClassNames+"> ");
            }

            final List<Class<?>> moduleSubClasses = findModuleSubClasses(moduleClass, allFileNames, classLoader);
            if (moduleSubClasses.isEmpty()) {
                throw new IllegalArgumentException("Failed to find subclasses for Module <"+moduleClass.getSimpleName()+">");
            }

            final Class<?> mostSpecificSubClass = findMostSpecificSubClass(moduleSubClasses);
            final Object module = Classes.newInstance(mostSpecificSubClass);
            if (module == null) {
                throw new IllegalArgumentException("Failed to instantiate Module class <"+moduleClass.getSimpleName()+">");
            }
            return load(mostSpecificSubClass, extractConnectionManagerClassName(mostSpecificSubClass.getPackage().getName(), moduleClass.getSimpleName(), module));
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

}