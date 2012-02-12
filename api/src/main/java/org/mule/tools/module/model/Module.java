package org.mule.tools.module.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        private final String type;
        private final List<Parameter> parameters;
        private final boolean intercepting;

        public Processor(final String name, final String type, final List<Parameter> parameters, final boolean intercepting) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (type == null) {
                throw new IllegalArgumentException("null type");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("null parameters");
            }

            this.name = name;
            this.type = type;
            this.parameters = parameters;
            this.intercepting = intercepting;
        }

        public final String getName() {
            return this.name;
        }

        public final String getType() {
            return this.type;
        }

        public final List<Parameter> getParameters() {
            return this.parameters;
        }

        public final boolean isIntercepting() {
            return this.intercepting;
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> type: <"+this.type+"> parameters: <"+this.parameters+"> intercepting: <"+this.intercepting+">";
        }

    }

    public static class Source {

        private final String name;
        private final String type;
        private final List<Parameter> parameters;

        public Source(final String name, final String type, final List<Parameter> parameters) {
            if (name == null) {
                throw new IllegalArgumentException("null name");
            }
            if (type == null) {
                throw new IllegalArgumentException("null type");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("null parameters");
            }

            this.name = name;
            this.type = type;
            this.parameters = parameters;
        }

        public final String getName() {
            return this.name;
        }

        public final String getType() {
            return this.type;
        }

        public final List<Parameter> getParameters() {
            return this.parameters;
        }

        @Override
        public String toString() {
            return "name: <"+this.name+"> type: <"+this.type+"> parameters: <"+this.parameters+">";
        }

    }

    /**
     * @see {@link org.mule.api.annotations.Transformer}
     */
    public static class Transformer {

        private final String type;
        private final int priorityWeighting;
        private final Class<?>[] sourceTypes;

        public Transformer(final String type, final int priorityWeighting, final Class<?>[] sourceTypes) {
            if (type == null) {
                throw new IllegalArgumentException("null type");
            }
            if (sourceTypes == null) {
                throw new IllegalArgumentException("null sourceTypes");
            }

            this.type = type;
            this.priorityWeighting = priorityWeighting;
            this.sourceTypes = sourceTypes;
        }

        public final String getType() {
            return this.type;
        }

        public final int getPriorityWeighting() {
            return this.priorityWeighting;
        }

        public final Class<?>[] getSourceTypes() {
            return this.sourceTypes;
        }

        @Override
        public String toString() {
            return "type: <"+this.type+"> priorityWeighting: <"+this.priorityWeighting+"> sourceTypes: <"+Arrays.toString(this.sourceTypes) +">";
        }

    }

    private final String name;
    private final String minMuleVersion;
    private final String type;
    private final List<Parameter> parameters;
    private final List<Processor> processors;
    private final List<Source> sources;
    private final List<Transformer> transformers;
    private final String connectionManagerType;

    public Module(final String name, final String minMuleVersion, final String type, final List<Parameter> parameters, final List<Processor> processors, final List<Source> sources, final List<Transformer> transformers, final String connectionManagerTypeName) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        if (minMuleVersion == null) {
            throw new IllegalArgumentException("null minMuleVersion");
        }
        if (type == null) {
            throw new IllegalArgumentException("null type");
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
        if (transformers == null) {
            throw new IllegalArgumentException("null transformers");
        }

        this.name = name;
        this.minMuleVersion = minMuleVersion;
        this.type = type;
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
        this.processors = Collections.unmodifiableList(new ArrayList<Processor>(processors));
        this.sources = Collections.unmodifiableList(new ArrayList<Source>(sources));
        this.transformers = Collections.unmodifiableList(new ArrayList<Transformer>(transformers));
        this.connectionManagerType = connectionManagerTypeName;
    }

    public final String getName() {
        return this.name;
    }

    public final String getMinMuleVersion() {
        return this.minMuleVersion;
    }

    public final String getType() {
        return this.type;
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

    public final List<Transformer> getTransformers() {
        return this.transformers;
    }

    public final String getConnectionManagerType() {
        return this.connectionManagerType;
    }

}