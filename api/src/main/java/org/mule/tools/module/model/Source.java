package org.mule.tools.module.model;

import com.google.common.base.Preconditions;
import java.util.List;

public class Source {

    private final String name;
    private final String type;
    private final List<Parameter> parameters;

    public Source(final String name, final String type, final List<Parameter> parameters) {
        Preconditions.checkNotNull(name, "null name");
        Preconditions.checkNotNull(type, "null type");
        Preconditions.checkNotNull(parameters, "null parameters");

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