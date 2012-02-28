package org.mule.tools.module.model;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;

/**
 * Model for {@link Processor} and {@link Module} parameter.
 *
 * @see {@link org.mule.api.annotations.param.Default}
 * @see {@link org.mule.api.annotations.param.Optional}
 */
public class Parameter {

    private final String name;
    private final Class<?> type;
    private final boolean optional;
    private final String defaultValue;

    public Parameter(final String name, final Class<?> type, final boolean optional, @Nullable final String defaultValue) {
        Preconditions.checkNotNull(name, "null name");
        Preconditions.checkNotNull(type, "null type");

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

    @Nullable
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
        return this.name.equals(module.getName());
    }

    @Override
    public String toString() {
        return "name: <"+this.name+"> optional: <"+this.optional+">"+(this.defaultValue != null?" default: <"+this.defaultValue+">":"");
    }

}