package org.mule.tools.module.loader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.ConnectionManager;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.processor.MessageProcessor;
import org.mule.tools.module.helper.Jars;
import org.mule.tools.module.model.Connector;
import org.mule.tools.module.model.Module;
import org.mule.util.StringUtils;

public class JarLoader {

    private static final Logger LOGGER = Logger.getLogger(JarLoader.class.getPackage().getName());

    private static final String MODULE_CLASS_SUFFIX = "Module";
    private static final String MESSAGE_PROCESSOR_CLASS_SUFFIX = "MessageProcessor";
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
            if (fileName.endsWith(JarLoader.MODULE_CLASS_SUFFIX+".class")) {
                potentialModuleClassNames.add(fileName);
            }
        }
        return potentialModuleClassNames;
    }

    protected final Class<?> findModuleClass(final List<String> allFileNames, final ClassLoader classLoader) {
        final List<String> potentialModuleClassNames = findPotentialModuleClassNames(allFileNames);
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
        //TODO Make sure we get the most specific "Module" sub-class.
        final String capabilitiesClassName = moduleClass.getName().replace(moduleClass.getSimpleName(), JarLoader.CONFIG_PACKAGE_PATH+moduleClass.getSimpleName())+JarLoader.CAPABILITIES_ADAPTER_CLASS_SUFFIX;
        return loadClass(classLoader, capabilitiesClassName);
    }

    protected final Class<?> findConnectionManagerClass(final Class<?> moduleClass, final ClassLoader classLoader) {
        final String connectionManagerClassName = moduleClass.getName().replace(moduleClass.getSimpleName(), JarLoader.CONFIG_PACKAGE_PATH+moduleClass.getSimpleName())+JarLoader.CONNECTION_MANAGER_CLASS_SUFFIX;
        return loadClass(classLoader, connectionManagerClassName);
    }

    protected final Class<?> findMessageProcessorClass(final Class<?> moduleClass, final String processorName, final ClassLoader classLoader) {
        final String messageProcessorName = moduleClass.getPackage().getName()+"."+JarLoader.CONFIG_PACKAGE_PATH+StringUtils.capitalize(processorName)+JarLoader.MESSAGE_PROCESSOR_CLASS_SUFFIX;
        return loadClass(classLoader, messageProcessorName);
    }

    public final Module load(final List<URL> urls) throws IOException {
        final URL moduleJar = urls.get(0);
        final List<String> allFileNames = Jars.allFileNames(moduleJar);
        final URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        final Class<?> moduleClass = findModuleClass(allFileNames, classLoader);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Failed to find Module class in <"+moduleJar+">");
        }
        final Object module = newInstance(moduleClass);
        if (module == null) {
            throw new IllegalArgumentException("Failed to instantiate Module class <"+moduleClass.getCanonicalName()+">");
        }
        final org.mule.api.annotations.Module moduleAnnotation = moduleClass.getAnnotation(org.mule.api.annotations.Module.class);
        if (moduleAnnotation != null) {
            return createModule(moduleAnnotation, module, moduleClass, classLoader);
        } else {
            return createConnector(moduleClass.getAnnotation(org.mule.api.annotations.Connector.class), module, moduleClass, classLoader);
        }
    }

    protected final String extractClassName(final String name) {
        final String strippedClassName = name.substring(0, name.lastIndexOf("."));
        return strippedClassName.replace('/', '.');
    }

    protected final Module createModule(final org.mule.api.annotations.Module annotation, final Object module, final Class<?> moduleClass, final ClassLoader classLoader) {
        final Class<?> capabilitiesClass = findCapabilitiesClass(moduleClass, classLoader);
        if (capabilitiesClass == null) {
            throw new IllegalArgumentException("Failed to find Capabilities class for <"+moduleClass+">");
        }
        final Capabilities capabilities = newInstance(capabilitiesClass);
        if (capabilities == null) {
            throw new IllegalArgumentException("Failed to instantiate Capabilities class <"+capabilitiesClass.getCanonicalName()+">");
        }
        return new Module(annotation.name(), module, capabilities, listParameters(moduleClass), listProcessors(moduleClass, classLoader), classLoader);
    }

    protected final Connector createConnector(final org.mule.api.annotations.Connector annotation, final Object module, final Class<?> moduleClass, final ClassLoader classLoader) {
        final Class<?> capabilitiesClass = findCapabilitiesClass(moduleClass, classLoader);
        if (capabilitiesClass == null) {
            throw new IllegalArgumentException("Failed to find Capabilities class for <"+moduleClass+">");
        }
        final Capabilities capabilities = newInstance(capabilitiesClass);
        if (capabilities == null) {
            throw new IllegalArgumentException("Failed to instantiate Capabilities class <"+capabilitiesClass.getCanonicalName()+">");
        }
        if (capabilities.isCapableOf(Capability.CONNECTION_MANAGEMENT_CAPABLE)) {
            final Class<?> connectionManagerClass = findConnectionManagerClass(moduleClass, classLoader);
            if (connectionManagerClass == null) {
                throw new IllegalArgumentException("Failed to find ConnectionManager class for connector <"+moduleClass+">");
            }
            final ConnectionManager<?, ?> connectionManager = newInstance(connectionManagerClass);
            if (connectionManager == null) {
                throw new IllegalArgumentException("Failed to instantiate ConnectionManager class <"+connectionManagerClass.getCanonicalName()+">");
            }
            return new Connector(annotation.name(), module, capabilities, listParameters(moduleClass), listProcessors(moduleClass, classLoader), connectionManager, classLoader);
        }
        return new Connector(annotation.name(), module, capabilities, listParameters(moduleClass), listProcessors(moduleClass, classLoader), classLoader);
    }
    
    protected final List<Connector.Parameter> listParameters(final Class<?> moduleClass) {
        final List<Connector.Parameter> parameters = new LinkedList<Connector.Parameter>();
        for (final Field field : moduleClass.getDeclaredFields()) {
            if (field.getAnnotation(Configurable.class) != null) {
                final boolean optional = field.getAnnotation(Optional.class) != null;
                final String defaultValue = field.getAnnotation(Default.class) != null ? field.getAnnotation(Default.class).value() : null;
                parameters.add(new Connector.Parameter(field.getName(), field.getType(), optional, defaultValue));
            }
        }
        return parameters;
    }

    protected final String extractProcessorName(final Processor annotation, final Method method) {
        if (!"".equals(annotation.name())) {
            return annotation.name();
        }
        return method.getName();
    }

    protected final String[] extractMethodParameterNames(final Method method, final MessageProcessor messageProcessor) {
        final List<String> parameterNames = new LinkedList<String>();
        for (final Field field : messageProcessor.getClass().getDeclaredFields()) {
            final String fieldName = field.getName();
            if (!(fieldName.startsWith(JarLoader.PARAMETER_TYPE_FIELD_PREFIX) && fieldName.endsWith(JarLoader.PARAMETER_TYPE_FIELD_SUFFIX))) {
                continue;
            }

            final String parameterName = StringUtils.uncapitalize(fieldName.substring(JarLoader.PARAMETER_TYPE_FIELD_PREFIX.length(), fieldName.length()-JarLoader.PARAMETER_TYPE_FIELD_SUFFIX.length()));
            parameterNames.add(parameterName);
        }
        return parameterNames.toArray(new String[parameterNames.size()]);
    }

    protected final List<Connector.Parameter> listProcessorParameters(final Class<?> moduleClass, final Method method, final MessageProcessor messageProcessor) {
        final List<Connector.Parameter> parameters = new LinkedList<Connector.Parameter>();
        final String[] parameterNames = extractMethodParameterNames(method, messageProcessor);
        final Class<?>[] parameterTypes = method.getParameterTypes();
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

            parameters.add(new Connector.Parameter(name, type, optional, defaultValue));
        }
        return parameters;
    }

    protected final List<Connector.Processor> listProcessors(final Class<?> moduleClass, final ClassLoader classLoader) {
        final List<Connector.Processor> processors = new LinkedList<Connector.Processor>();
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
                processors.add(new Connector.Processor(extractProcessorName(annotation, method), messageProcessor, listProcessorParameters(moduleClass, method, messageProcessor), annotation.intercepting()));
            }
        }
        return processors;
    }

}