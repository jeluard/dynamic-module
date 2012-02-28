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

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

/**
 * Helper methods for lifecycle interfaces.
 */
public final class LifeCycles {

    private LifeCycles() {
    }

    public static void initialise(final Object object) throws InitialisationException {
        if (object instanceof Initialisable) {
            Initialisable.class.cast(object).initialise();
        }
    }

    public static void start(final Object object) throws MuleException {
        if (object instanceof Startable) {
            Startable.class.cast(object).start();
        }
    }

    public static void stop(final Object object) throws MuleException {
        if (object instanceof Stoppable) {
            Stoppable.class.cast(object).stop();
        }
    }

    public static void dispose(final Object object) {
        if (object instanceof Disposable) {
            Disposable.class.cast(object).dispose();
        }
    }

}