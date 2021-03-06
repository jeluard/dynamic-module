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

import com.google.common.base.Preconditions;

public final class Strings {

    private Strings() {
    }

    public static String capitalize(final String string) {
        Preconditions.checkArgument(!com.google.common.base.Strings.isNullOrEmpty(string));

        return new StringBuilder(string.length()).append(Character.toTitleCase(string.charAt(0))).append(string.substring(1)).toString();
    }

    public static String uncapitalize(final String string) {
        Preconditions.checkArgument(!com.google.common.base.Strings.isNullOrEmpty(string));

        return new StringBuilder(string.length()).append(Character.toLowerCase(string.charAt(0))).append(string.substring(1)).toString();
    }

}