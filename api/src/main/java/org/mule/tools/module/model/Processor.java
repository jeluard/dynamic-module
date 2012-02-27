package org.mule.tools.module.model;

import com.google.common.base.Preconditions;

import java.util.List;

public class Processor {

    private final String name;
    private final String type;
    private final List<Parameter> parameters;
    private final String returnType;
    private final boolean intercepting;

    public Processor(final String name, final String type, final List<Parameter> parameters, final String returnType, final boolean intercepting) {
        Preconditions.checkNotNull(name, "null name");
        Preconditions.checkNotNull(type, "null type");
        Preconditions.checkNotNull(parameters, "null parameters");
        Preconditions.checkNotNull(returnType, "null returnType");

        this.name = name;
        this.type = type;
        this.parameters = parameters;
        this.returnType = returnType;
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

    public final String getReturnType() {
        return this.returnType;
    }

    public final boolean isIntercepting() {
        return this.intercepting;
    }

    @Override
    public String toString() {
        return "name: <"+this.name+"> type: <"+this.type+"> parameters: <"+this.parameters+"> returnType: <"+this.returnType+"> intercepting: <"+this.intercepting+">";
    }

}