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