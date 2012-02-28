package org.mule.tools.module.model;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Module {

    private final String name;
    private final String minMuleVersion;
    private final String type;
    private final List<Parameter> parameters;
    private final List<Processor> processors;
    private final List<Source> sources;
    private final List<Transformer> transformers;
    private final String connectionManagerType;

    public Module(final String name, final String minMuleVersion, final String type, final List<Parameter> parameters, final List<Processor> processors, final List<Source> sources, final List<Transformer> transformers, final String connectionManagerTypeName) {
        Preconditions.checkNotNull(name, "null name");
        Preconditions.checkNotNull(minMuleVersion, "null minMuleVersion");
        Preconditions.checkNotNull(type, "null type");
        Preconditions.checkNotNull(parameters, "null parameters");
        Preconditions.checkNotNull(processors, "null processors");
        Preconditions.checkNotNull(sources, "null sources");
        Preconditions.checkNotNull(transformers, "null transformers");

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