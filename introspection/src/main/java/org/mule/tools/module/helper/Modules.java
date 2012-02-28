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

public final class Modules {

    private static final String CAPABILITY_CLASS_NAME = "org.mule.api.Capability";
    private static final String CONNECTION_MANAGEMENT_CAPABILITY = "CONNECTION_MANAGEMENT_CAPABLE";
    private static final String LIFECYCLE_CAPABILITY = "LIFECYCLE_CAPABLE";
    public static final String SOURCE_CALLBACK_CLASS_NAME = "org.mule.api.callback.SourceCallback";

    private Modules() {
    }

    private static Object capability(final String name) {
        return Reflections.staticInvoke(Modules.CAPABILITY_CLASS_NAME, "valueOf", name);
    }

    private static boolean isCapableOf(final Object module, final Object capability) {
        return Reflections.<Boolean>invoke(module, "isCapableOf", capability);
    }

    public static boolean isConnectionManagementCapable(final Object module) {
        return Modules.isCapableOf(module, Modules.capability(Modules.CONNECTION_MANAGEMENT_CAPABILITY));
    }

    public static boolean isLifeCycleCapable(final Object module) {
        return Modules.isCapableOf(module, Modules.capability(Modules.LIFECYCLE_CAPABILITY));
    }

}