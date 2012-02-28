package org.mule.tools.module.loader;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import javax.annotation.Nullable;

import org.mule.tools.module.helper.*;
import org.mule.tools.module.model.Metadata;
import org.mule.tools.module.model.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class JarLoader {

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

    protected final String extractClassName(final String name) {
        final String strippedClassName = name.substring(0, name.lastIndexOf("."));
        return strippedClassName.replace('/', '.');
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

    protected final Metadata extractMetadata(final Repository repository, final Class<?> moduleType, final Module module) {
        final Map<Metadata.Icon, URL> icons = new EnumMap<Metadata.Icon, URL>(Metadata.Icon.class);
        for (final Metadata.Icon icon : Metadata.Icon.values()) {
            populateIcon(icons, icon, repository, moduleType, module);
        }
        return new Metadata(repository.getHomePage(), icons);
    }

    protected final void populateIcon(final Map<Metadata.Icon, URL> icons, final Metadata.Icon icon, final Repository repository, final Class<?> moduleType, final Module module) {
        final Object annotation = Annotations.getAnnotation(moduleType, Annotations.ICONS_ANNOTATION_CLASS_NAME);
        final String path;
        if (annotation != null) {
            path = Reflections.invoke(annotation, Cases.constantToCamelCase(icon.name()));
        } else {
            path = Reflections.staticGet(Classes.loadClass(Annotations.ICONS_ANNOTATION_CLASS_NAME), "GENERIC_"+icon.name());
        }
        final String finalPath = String.format(path, module.getName());
        final URL url = repository.getIconLocation(finalPath);
        if (url != null) {
            icons.put(icon, url);
        }
    }

    /**
     * @param urls
     * @return a {@link Module} representation of first module found in specified `urls`
     * @throws IOException 
     */
    public final org.mule.tools.module.model.Package load(final List<URL> urls) throws IOException {
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
            final Object moduleObject = Classes.newInstance(mostSpecificSubClass);
            if (moduleObject == null) {
                throw new IllegalArgumentException("Failed to instantiate Module class <"+moduleClass.getSimpleName()+">");
            }
            final Loader loader = new Loader();
            final Module module = loader.load(mostSpecificSubClass, extractConnectionManagerClassName(mostSpecificSubClass.getPackage().getName(), moduleClass.getSimpleName(), moduleObject));
            final Metadata metadata;
            try {
                final File pom = Jars.load(moduleJar, "META-INF/maven/.*/pom.xml");
                try {
                    final Document document = XML.load(pom);
                    final List<String> scmUrls = XML.extract(document, "/project/scm/url");
                    if (!urls.isEmpty()) {
                        final String scmUrl = scmUrls.get(0);
                        final Repository repository = getRepository(scmUrl);
                        metadata = repository != null ? extractMetadata(repository, mostSpecificSubClass, module) : null;
                    } else {
                        metadata = null;
                    }
                } finally {
                    pom.delete();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new org.mule.tools.module.model.Package(module, metadata);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    @Nullable
    protected final Repository getRepository(final String url) throws IOException {
        if (true) {
            return new GithubRepository(url);
        }
        return null;
    }

}