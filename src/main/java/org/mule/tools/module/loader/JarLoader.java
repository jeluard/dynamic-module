package org.mule.tools.module.loader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.ConnectionManager;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.tools.module.helper.Classes;
import org.mule.tools.module.helper.Jars;
import org.mule.tools.module.helper.Reflections;
import org.mule.tools.module.model.Module;
import org.mule.util.StringUtils;

public class JarLoader {

    private static final Logger LOGGER = Logger.getLogger(JarLoader.class.getPackage().getName());

    private static final String MODULE_CLASS_SUFFIX = "Module";
    private static final String CONNECTOR_CLASS_SUFFIX = "Connector";
    private static final String MESSAGE_PROCESSOR_CLASS_SUFFIX = "MessageProcessor";
    private static final String MESSAGE_SOURCE_CLASS_SUFFIX = "MessageSource";
    private static final String TRANSFORMER_CLASS_SUFFIX = "Transformer";
    private static final String CONNECTION_MANAGER_CLASS_SUFFIX = "ConnectionManager";
    private static final String CAPABILITIES_ADAPTER_CLASS_SUFFIX = "CapabilitiesAdapter";
    private static final String CONFIG_PACKAGE_PATH = "config.";
    private static final String PARAMETER_TYPE_FIELD_PREFIX = "_";
    private static final String PARAMETER_TYPE_FIELD_SUFFIX = "Type";

    /**
     * @param classLoader
     * @param name
     * @return loaded {@link Class} if any; null otherwise
     */
    protected final Class<?> loadClass(final ClassLoader classLoader, final String name) {
        try {
            return classLoader.loadClass(name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param <T>
     * @param clazz
     * @return new {@link Class} instance; null if instantiation fails
     */
    protected final <T> T newInstance(final Class<?> clazz) {
        try {
            return (T) clazz.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    protected final List<String> findPotentialModuleClassNames(final List<String> allFileNames) {
        final List<String> potentialModuleClassNames = new LinkedList<String>();
        for (final String fileName : allFileNames) {
            if (fileName.endsWith(JarLoader.MODULE_CLASS_SUFFIX+".class") ||
                    fileName.endsWith(JarLoader.CONNECTOR_CLASS_SUFFIX+".class")) {
                potentialModuleClassNames.add(fileName);
            }
        }
        return potentialModuleClassNames;
    }

    protected final Class<?> findModuleClass(final List<String> allFileNames, final ClassLoader classLoader) {
        final List<String> potentialModuleClassNames = findPotentialModuleClassNames(allFileNames);
        if (potentialModuleClassNames.isEmpty()) {
            throw new IllegalArgumentException("Failed to find potential Module class among <"+allFileNames+">");
        }
        for (final String potentialModuleClassName : potentialModuleClassNames) {
            final String className = extractClassName(potentialModuleClassName);
            final Class<?> moduleClass = loadClass(classLoader, className);
            if (moduleClass == null) {
                throw new IllegalArgumentException("Failed to load <"+className+">");
            }
            if (moduleClass.getAnnotation(org.mule.api.annotations.Module.class) == null && moduleClass.getAnnotation(org.mule.api.annotations.Connector.class) == null) {
                if (JarLoader.LOGGER.isLoggable(Level.WARNING)) {
                    JarLoader.LOGGER.log(Level.WARNING, "Skipping invalid module <{0}>", className);
                }

                continue;
            }

            return moduleClass;
        }
        return null;
    }

    protected final Class<?> findCapabilitiesClass(final Class<?> moduleClass, final ClassLoader classLoader) {
        final String capabilitiesClassName = moduleClass.getName().replace(moduleClass.getSimpleName(), JarLoader.CONFIG_PACKAGE_PATH+moduleClass.getSimpleName())+JarLoader.CAPABILITIES_ADAPTER_CLASS_SUFFIX;
        return loadClass(classLoader, capabilitiesClassName);
    }

    protected final Class<?> findConnectionManagerClass(final Class<?> moduleClass, final ClassLoader classLoader) {
        final String connectionManagerClassName = moduleClass.getName().replace(moduleClass.getSimpleName(), JarLoader.CONFIG_PACKAGE_PATH+moduleClass.getSimpleName())+JarLoader.CONNECTION_MANAGER_CLASS_SUFFIX;
        return loadClass(classLoader, connectionManagerClassName);
    }

    protected final Class<?> findMessageProcessorClass(final Class<?> moduleClass, final String processorName, final ClassLoader classLoader) {
        final String messageProcessorClassName = moduleClass.getPackage().getName()+"."+JarLoader.CONFIG_PACKAGE_PATH+StringUtils.capitalize(processorName)+JarLoader.MESSAGE_PROCESSOR_CLASS_SUFFIX;
        return loadClass(classLoader, messageProcessorClassName);
    }

    protected final Class<?> findMessageSourceClass(final Class<?> moduleClass, final String sourceName, final ClassLoader classLoader) {
        final String messageSourceName = moduleClass.getPackage().getName()+"."+JarLoader.CONFIG_PACKAGE_PATH+StringUtils.capitalize(sourceName)+JarLoader.MESSAGE_SOURCE_CLASS_SUFFIX;
        return loadClass(classLoader, messageSourceName);
    }

    protected final Class<?> findTransformerClass(final Class<?> moduleClass, final String transformerName, final ClassLoader classLoader) {
        final String transformerClassName = moduleClass.getPackage().getName()+"."+JarLoader.CONFIG_PACKAGE_PATH+StringUtils.capitalize(transformerName)+JarLoader.TRANSFORMER_CLASS_SUFFIX;
        return loadClass(classLoader, transformerClassName);
    }

    protected final List<Class<?>> findModuleSubClasses(final Class<?> moduleClass, final List<String> allFileNames, final URLClassLoader classLoader) {
        final String moduleClassSimpleName = moduleClass.getSimpleName();
        final List<Class<?>> subClasses = new LinkedList<Class<?>>();
        for (final String fileName : allFileNames) {
            if (fileName.contains(moduleClassSimpleName)) {
                final Class<?> clazz = loadClass(classLoader, extractClassName(fileName));
                if (Classes.allSuperClasses(clazz).contains(moduleClass)) {
                    subClasses.add(clazz);
                }
            }
        }
        return subClasses;
    }

    protected final Class<?> findMostSpecificSubClass(final List<Class<?>> moduleSubClasses) {
        return Collections.max(moduleSubClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(final Class<?> class1, final Class<?> class2) {
                return Integer.valueOf(Classes.allSuperClasses(class1).size()).compareTo(Classes.allSuperClasses(class2).size());
            }
        });
    }

    protected final Object extractAnnotation(final Class<?> moduleClass) {
        final org.mule.api.annotations.Module moduleAnnotation = moduleClass.getAnnotation(org.mule.api.annotations.Module.class);
        if (moduleAnnotation != null) {
            return moduleAnnotation;
        } else {
            return moduleClass.getAnnotation(org.mule.api.annotations.Connector.class);
        }
    }

    public final Module load(final List<URL> urls) throws IOException {
        final URL moduleJar = urls.get(0);
        final List<String> allFileNames = Jars.allFileNames(moduleJar);
        final URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        final Class<?> moduleClass = findModuleClass(allFileNames, classLoader);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Failed to find Module class in <"+moduleJar+">");
        }
        final List<Class<?>> moduleSubClasses = findModuleSubClasses(moduleClass, allFileNames, classLoader);
        final Class<?> mostSpecificSubClass = findMostSpecificSubClass(moduleSubClasses);
        final Object module = newInstance(mostSpecificSubClass);
        if (module == null) {
            throw new IllegalArgumentException("Failed to instantiate Module class <"+moduleClass.getCanonicalName()+">");
        }
        return createModule(extractAnnotation(moduleClass), module, moduleClass, classLoader);
    }

    protected final String extractClassName(final String name) {
        final String strippedClassName = name.substring(0, name.lastIndexOf("."));
        return strippedClassName.replace('/', '.');
    }

    protected final ConnectionManager<?, ?> extractConnectionManager(final Class<?> moduleClass, final Capabilities capabilities, final ClassLoader classLoader) {
        if (capabilities.isCapableOf(Capability.CONNECTION_MANAGEMENT_CAPABLE)) {
            final Class<?> connectionManagerClass = findConnectionManagerClass(moduleClass, classLoader);
            if (connectionManagerClass == null) {
                throw new IllegalArgumentException("Failed to find ConnectionManager class for connector <"+moduleClass+">");
            }
            final ConnectionManager<?, ?> connectionManager = newInstance(connectionManagerClass);
            if (connectionManager == null) {
                throw new IllegalArgumentException("Failed to instantiate ConnectionManager class <"+connectionManagerClass.getCanonicalName()+">");
            }
            return connectionManager;
        }
        return null;
    }

    protected final String extractName(final Object annotation) {
        return Reflections.invoke(annotation, "name");
    }

    protected final String extractMinMuleVersion(final Object annotation) {
        return Reflections.invoke(annotation, "minMuleVersion");
    }

    protected final Module createModule(final Object annotation, final Object module, final Class<?> moduleClass, final ClassLoader classLoader) {
        final Class<?> capabilitiesClass = findCapabilitiesClass(moduleClass, classLoader);
        if (capabilitiesClass == null) {
            throw new IllegalArgumentException("Failed to find Capabilities class for <"+moduleClass+">");
        }
        final Capabilities capabilities = newInstance(capabilitiesClass);
        if (capabilities == null) {
            throw new IllegalArgumentException("Failed to instantiate Capabilities class <"+capabilitiesClass.getCanonicalName()+">");
        }
        return new Module(extractName(annotation), extractMinMuleVersion(annotation), module, capabilities, listParameters(moduleClass), listProcessors(moduleClass, classLoader), listSources(moduleClass, classLoader), listTransformers(moduleClass, classLoader), extractConnectionManager(moduleClass, capabilities, classLoader), classLoader);
    }
    
    protected final List<Module.Parameter> listParameters(final Class<?> moduleClass) {
        final List<Module.Parameter> parameters = new LinkedList<Module.Parameter>();
        for (final Field field : moduleClass.getDeclaredFields()) {
            if (field.getAnnotation(Configurable.class) != null) {
                final boolean optional = field.getAnnotation(Optional.class) != null;
                final String defaultValue = field.getAnnotation(Default.class) != null ? field.getAnnotation(Default.class).value() : null;
                parameters.add(new Module.Parameter(field.getName(), field.getType(), optional, defaultValue));
            }
        }
        return parameters;
    }

    protected final String methodNameToDashBased(final Method method) {
        final String methodName = method.getName();
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < methodName.length(); i++) {
            final char character = methodName.charAt(i);
            if (Character.isUpperCase(character)) {
                builder.append("-").append(Character.toLowerCase(character));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    protected final String extractProcessorName(final Processor annotation, final Method method) {
        if (!"".equals(annotation.name())) {
            return annotation.name();
        }

        return methodNameToDashBased(method);
    }

    protected final String extractSourceName(final Source annotation, final Method method) {
        if (!"".equals(annotation.name())) {
            return annotation.name();
        }

        return methodNameToDashBased(method);
    }

    protected final String[] extractMethodParameterNames(final Class<?> generatedClass) {
        final List<String> parameterNames = new LinkedList<String>();
        for (final Field field : generatedClass.getDeclaredFields()) {
            final String fieldName = field.getName();
            if (!(fieldName.startsWith(JarLoader.PARAMETER_TYPE_FIELD_PREFIX) && fieldName.endsWith(JarLoader.PARAMETER_TYPE_FIELD_SUFFIX))) {
                continue;
            }

            final String parameterName = StringUtils.uncapitalize(fieldName.substring(JarLoader.PARAMETER_TYPE_FIELD_PREFIX.length(), fieldName.length()-JarLoader.PARAMETER_TYPE_FIELD_SUFFIX.length()));
            parameterNames.add(parameterName);
        }
        return parameterNames.toArray(new String[parameterNames.size()]);
    }

    protected final Class<?>[] extractMethodParameterTypes(final Method method) {
        final List<Class<?>> parameterTypes = new LinkedList<Class<?>>();
        for (final Class<?> type : method.getParameterTypes()) {
            //SourceCallback is not a user parameter.
            if (SourceCallback.class.equals(type)) {
                continue;
            }

            parameterTypes.add(type);
        }
        return parameterTypes.toArray(new Class<?>[parameterTypes.size()]);
    }

    protected final List<Module.Parameter> listMethodParameters(final Class<?> moduleClass, final Method method, final Class<?> generatedClass) {
        final List<Module.Parameter> parameters = new LinkedList<Module.Parameter>();
        //Rely on the fact that parameters are added first in generated MessageProcessor/MessageSource.
        //TODO Pretty fragile. Replace with stronger alternative.
        final String[] parameterNames = extractMethodParameterNames(generatedClass);
        final Class<?>[] parameterTypes = extractMethodParameterTypes(method);
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterTypes.length; i++) {
            final String name = parameterNames[i];
            final Class<?> type = parameterTypes[i];
            final List<Annotation> annotations = Arrays.asList(parameterAnnotations[i]);
            boolean optional = false;
            String defaultValue = null;
            for (final Annotation annotation : annotations) {
                if (annotation instanceof Optional) {
                    optional = true;
                }
                if (annotation instanceof Default) {
                    defaultValue = Default.class.cast(annotation).value();
                }
            }

            parameters.add(new Module.Parameter(name, type, optional, defaultValue));
        }
        return parameters;
    }

    protected final List<Module.Processor> listProcessors(final Class<?> moduleClass, final ClassLoader classLoader) {
        final List<Module.Processor> processors = new LinkedList<Module.Processor>();
        for (final Method method : moduleClass.getMethods()) {
            final Processor annotation = method.getAnnotation(Processor.class);
            if (annotation != null) {
                final Class<?> messageProcessorClass = findMessageProcessorClass(moduleClass, method.getName(), classLoader);
                if (messageProcessorClass == null) {
                    throw new IllegalArgumentException("Failed to find MessageProcessor class for processor <"+method.getName()+">");
                }
                final MessageProcessor messageProcessor = newInstance(messageProcessorClass);
                if (messageProcessor == null) {
                    throw new IllegalArgumentException("Failed to instantiate MessageProcessor class <"+messageProcessorClass.getCanonicalName()+">");
                }
                processors.add(new Module.Processor(extractProcessorName(annotation, method), messageProcessor, listMethodParameters(moduleClass, method, messageProcessorClass), annotation.intercepting()));
            }
        }
        return processors;
    }

    protected final List<Module.Source> listSources(final Class<?> moduleClass, final ClassLoader classLoader) {
        final List<Module.Source> sources = new LinkedList<Module.Source>();
        for (final Method method : moduleClass.getMethods()) {
            final Source annotation = method.getAnnotation(Source.class);
            if (annotation != null) {
                final Class<?> messageSourceClass = findMessageSourceClass(moduleClass, method.getName(), classLoader);
                if (messageSourceClass == null) {
                    throw new IllegalArgumentException("Failed to find MessageSource class for processor <"+method.getName()+">");
                }
                final MessageSource messageSource = newInstance(messageSourceClass);
                if (messageSource == null) {
                    throw new IllegalArgumentException("Failed to instantiate MessageSource class <"+messageSourceClass.getCanonicalName()+">");
                }
                sources.add(new Module.Source(extractSourceName(annotation, method), messageSource, listMethodParameters(moduleClass, method, messageSourceClass)));
            }
        }
        return sources;
    }

    protected final List<Module.Transformer> listTransformers(final Class<?> moduleClass, final ClassLoader classLoader) {
        final List<Module.Transformer> transformers = new LinkedList<Module.Transformer>();
        for (final Method method : moduleClass.getMethods()) {
            final Transformer annotation = method.getAnnotation(Transformer.class);
            if (annotation != null) {
                final Class<?> transformerClass = findTransformerClass(moduleClass, method.getName(), classLoader);
                if (transformerClass == null) {
                    throw new IllegalArgumentException("Failed to find Transformer class for processor <"+method.getName()+">");
                }
                final org.mule.api.transformer.Transformer transformer = newInstance(transformerClass);
                if (transformer == null) {
                    throw new IllegalArgumentException("Failed to instantiate Transformer class <"+transformerClass.getCanonicalName()+">");
                }
                transformers.add(new Module.Transformer(transformer, annotation.priorityWeighting(), annotation.sourceTypes()));
            }
        }
        return transformers;
    }

}