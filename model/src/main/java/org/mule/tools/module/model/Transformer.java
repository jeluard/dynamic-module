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