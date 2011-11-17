package org.mule.tools.module.loader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
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
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                if (JarLoader.LOGGER.isLoggable(Level.FINE)) {
                    JarLoader.LOGGER.log(Level.FINE, "Failed to load <{0}>{1}", new Object[]{className, e.toString()});
                }

                continue;
            }
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
            e.printStackTrace();
            return null;
        }
    }

    public Module load(final List<URL> urls) throws IOException {
        final List<String> allFileNames = Jars.allFileNames(urls.get(0));
        final ClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        final Class<?> moduleClass = findModuleClass(allFileNames, classLoader);
        final Object module;
        try {
            module = moduleClass.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate Module class <"+moduleClass.getCanonicalName()+">", e);
        }
        final org.mule.api.annotations.Module moduleAnnotation = moduleClass.getAnnotation(org.mule.api.annotations.Module.class);
        if (moduleAnnotation != null) {
            return createModule(moduleAnnotation, module, moduleClass, classLoader);
        }
        final org.mule.api.annotations.Connector connectorAnnotation = moduleClass.getAnnotation(org.mule.api.annotations.Connector.class);
        if (connectorAnnotation != null) {
            return createConnector(connectorAnnotation, module, moduleClass, classLoader);
        }
        throw new IllegalArgumentException("Cannot find either @Module or @Connector annotation on <"+moduleClass+">");
    }

    protected final String extractClassName(final String name) {
        final String strippedClassName = name.substring(0, name.lastIndexOf("."));
        return strippedClassName.replaceAll("/", ".");
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
                processors.add(new Connector.Processor(extractProcessorname(annotation, method), messageProcessor, annotation.intercepting()));
            }
        }
        return processors;
    }

}