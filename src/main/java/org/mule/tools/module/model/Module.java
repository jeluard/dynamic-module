package org.mule.tools.module.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mule.api.Capabilities;
import org.mule.api.processor.MessageProcessor;

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
        private final boolean intercepting;

        public Processor(final String name, final MessageProcessor messageProcessor, final boolean intercepting) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (messageProcessor == null) {
                throw new IllegalArgumentException("null messageProcessor");
            }

            this.name = name;
            this.messageProcessor = messageProcessor;
            this.intercepting = intercepting;
        }

        public final String getName() {
            return this.name;
        }

        public final MessageProcessor getMessageProcessor() {
            return this.messageProcessor;
        }

        public final boolean isIntercepting() {
            return this.intercepting;
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> type: <"+this.messageProcessor.getClass().getName()+"> intercepting: <"+this.intercepting+">";
        }

    }

    private final String name;
    private final Object module;
    private final Capabilities capabilities;
    private final List<Parameter> parameters;
    private final List<Processor> processors;
    private final ClassLoader classLoader;

    public Module(final String name, final Object module, final Capabilities capabilities, final List<Parameter> parameters, final List<Processor> processors, final ClassLoader classLoader) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
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
        if (classLoader == null) {
            throw new IllegalArgumentException("null classLoader");
        }

        this.name = name;
        this.module = module;
        this.capabilities = capabilities;
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
        this.processors = Collections.unmodifiableList(new ArrayList<Processor>(processors));
        this.classLoader = classLoader;
    }

    public final String getName() {
        return this.name;
    }

    public final Object getModule() {
        return this.module;
    }

    public Object getModuleObject() {
        return getModule();
    }

    public final Capabilities getCapabilities() {
        return this.capabilities;
    }

    /**
     * @param name
     * @return {@link Parameter} with specified name, null if none can be found
     */
    public final Parameter getParameter(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }

        for (final Parameter parameter : this.parameters) {
            if (name.equals(parameter.getName())) {
                return parameter;
            }
        }
        return null;
    }

    public final List<Parameter> getParameters() {
        return this.parameters;
    }

    public final List<Processor> getProcessors() {
        return this.processors;
    }

    public final ClassLoader getClassLoader() {
        return this.classLoader;
    }

}