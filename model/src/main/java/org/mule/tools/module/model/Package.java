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

public class Package {

    private final Module module;
    private final Metadata metadata;

    public Package(final Module module, final Metadata metadata) {
        Preconditions.checkNotNull(module, "null module");
        Preconditions.checkNotNull(metadata, "null metadata");

        this.module = module;
        this.metadata = metadata;
    }

    public final Module getModule() {
        return this.module;
    }

    public final Metadata getMetadata() {
        return this.metadata;
    }

}