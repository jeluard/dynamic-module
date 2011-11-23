package org.mule.tools.module.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.ConnectionManager;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.tools.module.helper.ConnectionManagers;

//TODO Add support for OAuth1 and OAuth2
public class Module {

    /**
     * @see {@link org.mule.api.annotations.param.Default}
     * @see {@link org.mule.api.annotations.param.Optional}
     */
    public static class Parameter {

        private final String name;
        private final Class<?> type;
        private final boolean optional;
        private final String defaultValue;

        public Parameter(final String name, final Class<?> type, final boolean optional, final String defaultValue) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (type == null) {
                throw new IllegalArgumentException("null type");
            }

            this.name = name;
            this.type = type;
            this.optional = optional;
            this.defaultValue = defaultValue;
        }

        public final String getName() {
            return this.name;
        }

        public final Class<?> getType() {
            return this.type;
        }

        public final boolean isOptional() {
            return this.optional;
        }

        public final String getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof Parameter)) {
                return false;
            }

            final Module module = (Module) other;
            return this.name.equals(module.name);
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> optional: <"+this.optional+">"+(this.defaultValue != null?" default: <"+this.defaultValue+">":"");
        }

    }

    public static class Processor {

        private final String name;
        private final MessageProcessor messageProcessor;
        private final List<Parameter> parameters;
        private final boolean intercepting;

        public Processor(final String name, final MessageProcessor messageProcessor, final List<Parameter> parameters, final boolean intercepting) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (messageProcessor == null) {
                throw new IllegalArgumentException("null messageProcessor");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("null parameters");
            }

            this.name = name;
            this.messageProcessor = messageProcessor;
            this.parameters = parameters;
            this.intercepting = intercepting;
        }

        public final String getName() {
            return this.name;
        }

        public final MessageProcessor getMessageProcessor() {
            return this.messageProcessor;
        }

        public final List<Parameter> getParameters() {
            return this.parameters;
        }

        public final boolean isIntercepting() {
            return this.intercepting;
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> type: <"+this.messageProcessor.getClass().getName()+"> parameters: <"+this.parameters+"> intercepting: <"+this.intercepting+">";
        }

    }

    public static class Source {

        private final String name;
        private final MessageSource messageSource;
        private final List<Parameter> parameters;

        public Source(final String name, final MessageSource messageSource, final List<Parameter> parameters) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (messageSource == null) {
                throw new IllegalArgumentException("null messageSource");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("null parameters");
            }

            this.name = name;
            this.messageSource = messageSource;
            this.parameters = parameters;
        }

        public final String getName() {
            return this.name;
        }

        public final MessageSource getMessageSource() {
            return this.messageSource;
        }

        public final List<Parameter> getParameters() {
            return this.parameters;
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> type: <"+this.messageSource.getClass().getName()+"> parameters: <"+this.parameters+">";
        }

    }

    private final String name;
    private final String minMuleVersion;
    private final Object module;
    private final Capabilities capabilities;
    private final List<Parameter> parameters;
    private final List<Processor> processors;
    private final List<Source> sources;
    private final ClassLoader classLoader;
    private final ConnectionManager<?, ?> connectionManager;

    public Module(final String name, final String minMuleVersion, final Object module, final Capabilities capabilities, final List<Parameter> parameters, final List<Processor> processors, final List<Source> sources, final ConnectionManager<?, ?> connectionManager, final ClassLoader classLoader) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        if (minMuleVersion == null) {
            throw new IllegalArgumentException("null minMuleVersion");
        }
        if (module == null) {
            throw new IllegalArgumentException("null modules");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("null capabilities");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("null parameters");
        }
        if (processors == null) {
            throw new IllegalArgumentException("null processors");
        }
        if (sources == null) {
            throw new IllegalArgumentException("null sources");
        }
        if (classLoader == null) {
            throw new IllegalArgumentException("null classLoader");
        }
        if (connectionManager == null) {
            throw new IllegalArgumentException("null connectionManager");
        }

        this.name = name;
        this.minMuleVersion = minMuleVersion;
        this.module = module;
        this.capabilities = capabilities;
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
        this.processors = Collections.unmodifiableList(new ArrayList<Processor>(processors));
        this.sources = sources;
        this.classLoader = classLoader;
        this.connectionManager = connectionManager;

        ensureConnectionManagementCapability();
    }

    protected final void ensureCapability(final Capability capability) {
        if (!this.capabilities.isCapableOf(capability)) {
            throw new IllegalArgumentException(Capabilities.class.getSimpleName()+" does not support "+Capability.CONNECTION_MANAGEMENT_CAPABLE);
        }
    }

    protected final void ensureConnectionManagementCapability() {
        ensureCapability(Capability.CONNECTION_MANAGEMENT_CAPABLE);
    }

    public final String getName() {
        return this.name;
    }

    public final String getMinMuleVersion() {
        return this.minMuleVersion;
    }

    public final Object getModule() {
        return this.module;
    }

    public Object getModuleObject() {
        if (getConnectionManager() != null) {
            return getConnectionManager();
        }
        return getModule();
    }

    public final Capabilities getCapabilities() {
        return this.capabilities;
    }

    public final List<Parameter> getParameters() {
        return this.parameters;
    }

    public final List<Processor> getProcessors() {
        return this.processors;
    }

    public final List<Source> getSources() {
        return this.sources;
    }

    public final ConnectionManager<?, ?> getConnectionManager() {
        return this.connectionManager;
    }

    public final ClassLoader getClassLoader() {
        return this.classLoader;
    }

    public final void setUsername(final String username) {
        ensureConnectionManagementCapability();

        ConnectionManagers.setUsername(this.connectionManager, username);
    }

    public final void setPassword(final String password) {
        ensureConnectionManagementCapability();

        ConnectionManagers.setPassword(this.connectionManager, password);
    }

    public final void setSecurityToken(final String securityToken) {
        ensureConnectionManagementCapability();

        ConnectionManagers.setSecurityToken(this.connectionManager, securityToken);
    }

}