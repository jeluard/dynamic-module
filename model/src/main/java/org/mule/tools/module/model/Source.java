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

import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Model for {@link org.mule.api.annotations.Source}.
 */
@Immutable
public class Source {

    private final String name;
    private final String friendlyName;
    private final String type;
    private final List<Parameter> parameters;

    public Source(final String name, @Nullable final String friendlyName, final String type, final List<Parameter> parameters) {
        Preconditions.checkNotNull(name, "null name");
        Preconditions.checkNotNull(type, "null type");
        Preconditions.checkNotNull(parameters, "null parameters");

        this.name = name;
        this.friendlyName = friendlyName;
        this.type = type;
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
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

    @Override
    public String toString() {
        return "name: <"+this.name+"> friendlyName: <"+this.friendlyName+"> type: <"+this.type+"> parameters: <"+this.parameters+">";
    }

}