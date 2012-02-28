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

import java.net.URL;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Metadata {

    public enum Icon {
        CLOUD_CONNECTOR_SMALL,
        CLOUD_CONNECTOR_LARGE,
        TRANSFORMER_SMALL,
        TRANSFORMER_LARGE,
        ENDPOINT_SMALL,
        ENDPOINT_LARGE
    }

    private final URL homepage;
    private final Map<Icon, URL> icons;

    public Metadata(final URL homepage, final Map<Icon, URL> icons) {
        Preconditions.checkNotNull(homepage, "null homepage");
        Preconditions.checkNotNull(icons, "null icons");

        this.homepage = homepage;
        this.icons = Collections.unmodifiableMap(new EnumMap<Icon, URL>(icons));
    }

    public final URL getHomepage() {
        return this.homepage;
    }

    public final Map<Icon, URL> getIcons() {
        return this.icons;
    }

}