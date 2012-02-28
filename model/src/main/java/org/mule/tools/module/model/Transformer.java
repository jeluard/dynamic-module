package org.mule.tools.module.model;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import javax.annotation.concurrent.Immutable;

/**
 * Model for {@link org.mule.api.annotations.Transformer}.
 */
@Immutable
public class Transformer {

    private final String type;
    private final int priorityWeighting;
    private final Class<?>[] sourceTypes;

    public Transformer(final String type, final int priorityWeighting, final Class<?>[] sourceTypes) {
        Preconditions.checkNotNull(type, "null type");
        Preconditions.checkNotNull(sourceTypes, "null sourceTypes");

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