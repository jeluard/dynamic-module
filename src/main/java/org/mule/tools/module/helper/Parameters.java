package org.mule.tools.module.helper;

import java.util.List;

import org.mule.tools.module.model.Module.Parameter;

/**
 * Helper methods for {@link Parameter}.
 */
public final class Parameters {

    /**
     * @param name
     * @return {@link Parameter} with specified name, null if none can be found
     */
    public static Parameter getParameter(final List<Parameter> parameters, final String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }

        for (final Parameter parameter : parameters) {
            if (name.equals(parameter.getName())) {
                return parameter;
            }
        }
        return null;
    }

}