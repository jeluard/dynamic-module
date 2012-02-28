package org.mule.tools.module.model;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Model for {@link org.mule.api.annotations.Processor}.
 */
@Immutable
public class Processor {

    private final String name;
    private final String friendlyName;
    private final String type;
    private final List<Parameter> parameters;
    private final String returnType;
    private final boolean intercepting;

    public Processor(final String name, @Nullable final String friendlyName, final String type, final List<Parameter> parameters, final String returnType, final boolean intercepting) {
        Preconditions.checkNotNull(name, "null name");
        Preconditions.checkNotNull(type, "null type");
        Preconditions.checkNotNull(parameters, "null parameters");
        Preconditions.checkNotNull(returnType, "null returnType");

        this.name = name;
        this.friendlyName = friendlyName;
        this.type = type;
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
        this.returnType = returnType;
        this.intercepting = intercepting;
    }

    public final String getName() {
        return this.name;
    }

    @Nullable
    public final String getFriendlyName() {
        return this.friendlyName;
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
        return "name: <"+this.name+"> friendlyName: <"+this.friendlyName+"> type: <"+this.type+"> parameters: <"+this.parameters+"> returnType: <"+this.returnType+"> intercepting: <"+this.intercepting+">";
    }

}