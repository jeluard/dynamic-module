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