/**
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2012 Julien Eluard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.mule.tools.module.helper;

import java.util.List;

import org.mule.tools.module.model.Parameter;

/**
 * Helper methods for {@link Parameter}.
 */
public final class Parameters {

    /**
     * @param name
     * @return {@link Parameter} with specified name, null if none can be found
     */
    public static org.mule.tools.module.model.Parameter getParameter(final List<Parameter> parameters, final String name) {
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