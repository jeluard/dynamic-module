package org.mule.tools.module.loader;

import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.Paranamer;

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

    private final Paranamer paranamer;
    private static final Paranamer DEFAULT_PARANAMER = new BytecodeReadingParanamer();

    public JarLoader() {
        this(JarLoader.DEFAULT_PARANAMER);
    }

    public JarLoader(final Paranamer paranamer) {
        this.paranamer = paranamer;
    }

    protected final List<String> findPotentialModuleClassNames(final List<String> allFileNames) {
        final List<String> potentialModuleClassNames = new LinkedList<String>();
        for (final String fileName : allFileNames) {
            if (fileName.endsWith("Module.class")) {
                potentialModuleClassNames.add(fileName);
            }
        }
        return potentialModuleClassNames;
    }

    protected final Class<?> findModuleClass(final List<String> allFileNames, final ClassLoader classLoader) {
        final List<String> potentialModuleClassNames = findPotentialModuleClassNames(allFileNames);
        for (final String potentialModuleClassName : potentialModuleClassNames) {
            final String className = extractClassName(potentialModuleClassName);
            final Class<?> moduleClass;
            try {
                moduleClass = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Failed to load <"+className+">", e);
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
        final String capabilitiesClassName = moduleClass.getName().replace(moduleClass.getSimpleName(), "config."+moduleClass.getSimpleName()) +"CapabilitiesAdapter";
        try {
            return classLoader.loadClass(capabilitiesClassName);
        } catch (Exception e) {
            return null;
        }
    }

    protected final Class<?> findConnectionManagerClass(final Class<?> moduleClass, final ClassLoader classLoader) {
        final String capabilitiesClassName = moduleClass.getName().replace(moduleClass.getSimpleName(), "config."+moduleClass.getSimpleName()) +"ConnectionManager";
        try {
            return classLoader.loadClass(capabilitiesClassName);
        } catch (Exception e) {
            return null;
        }
    }

    protected final Class<?> findMessageProcessorClass(final Class<?> moduleClass, final String processorName, final ClassLoader classLoader) {
        final String messageProcessorName = moduleClass.getPackage().getName()+".config."+StringUtils.capitalize(processorName)+"MessageProcessor";
        try {
            return classLoader.loadClass(messageProcessorName);
        } catch (Exception e) {
            return null;
        }
    }

    public Module load(final List<URL> urls) throws IOException {
        final URL moduleJar = urls.get(0);
        final List<String> allFileNames = Jars.allFileNames(moduleJar);
        final URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        final Class<?> moduleClass = findModuleClass(allFileNames, classLoader);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Failed to find Module class in <"+moduleJar+">");
        }
        final Object module;
        try {
            module = moduleClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate Module class <"+moduleClass.getCanonicalName()+">", e);
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
        final Capabilities capabilities;
        try {
            capabilities = (Capabilities) capabilitiesClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate Capabilities class <"+capabilitiesClass.getCanonicalName()+">", e);
        }
        return new Module(annotation.name(), module, capabilities, listParameters(moduleClass), listProcessors(moduleClass, classLoader), classLoader);
    }

    protected final Connector createConnector(final org.mule.api.annotations.Connector annotation, final Object module, final Class<?> moduleClass, final ClassLoader classLoader) {
        final Class<?> capabilitiesClass = findCapabilitiesClass(moduleClass, classLoader);
        if (capabilitiesClass == null) {
            throw new IllegalArgumentException("Failed to find Capabilities class for <"+moduleClass+">");
        }
        final Capabilities capabilities;
        try {
            capabilities = (Capabilities) capabilitiesClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate Capabilities class <"+capabilitiesClass.getCanonicalName()+">", e);
        }
        if (capabilities.isCapableOf(Capability.CONNECTION_MANAGEMENT_CAPABLE)) {
            final Class<?> connectionManagerClass = findConnectionManagerClass(moduleClass, classLoader);
            if (connectionManagerClass == null) {
                throw new IllegalArgumentException("Failed to find ConnectionManager class for connector <"+moduleClass+">");
            }
            final ConnectionManager<?, ?> connectionManager;
            try {
                connectionManager = (ConnectionManager<?, ?>) connectionManagerClass.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to instantiate ConnectionManager class <"+connectionManagerClass.getCanonicalName()+">", e);
            }
            return new Connector(annotation.name(), module, capabilities, listParameters(moduleClass), listProcessors(moduleClass, classLoader), connectionManager, classLoader);
        }
        return new Connector(annotation.name(), module, capabilities, listParameters(moduleClass), listProcessors(moduleClass, classLoader), classLoader);
    }
    
    protected final List<Connector.Parameter> listParameters(final Class<?> moduleClass) {
        //TODO on getter/setter level?
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

    protected final String extractProcessorname(final Processor annotation, final Method method) {
        if (!"".equals(annotation.name())) {
            return annotation.name();
        }
        return method.getName();
    }

    protected final String[] extractMethodParameterNames(final Method method) {
        final String[] parameterNames = this.paranamer.lookupParameterNames(method, false);
        if (parameterNames != null) {
            return parameterNames;
        }

        //Fall back to type inferred names
        final List<String> inferredParameterNames = new LinkedList<String>();
        for (final Class<?> type : method.getParameterTypes()) {
            inferredParameterNames.add(type.getSimpleName());
        }
        return inferredParameterNames.toArray(new String[inferredParameterNames.size()]);
    }

    protected final List<Connector.Parameter> listProcessorParameters(final Class<?> moduleClass, final Method method) {
        final List<Connector.Parameter> parameters = new LinkedList<Connector.Parameter>();
        final String[] parameterNames = extractMethodParameterNames(method);
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
                final MessageProcessor messageProcessor;
                try {
                    messageProcessor = (MessageProcessor) messageProcessorClass.newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to instantiate MessageProcessor class <"+messageProcessorClass.getCanonicalName()+">", e);
                }
                processors.add(new Connector.Processor(extractProcessorname(annotation, method), messageProcessor, listProcessorParameters(moduleClass, method), annotation.intercepting()));
            }
        }
        return processors;
    }

}