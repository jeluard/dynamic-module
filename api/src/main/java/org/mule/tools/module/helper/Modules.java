package org.mule.tools.module.helper;

public final class Modules {

    private static final String CAPABILITY_CLASS_NAME = "org.mule.api.Capability";
    private static final String CONNECTION_MANAGEMENT_CAPABILITY = "CONNECTION_MANAGEMENT_CAPABLE";
    private static final String LIFECYCLE_CAPABILITY = "LIFECYCLE_CAPABLE";
    public static final String SOURCE_CALLBACK_CLASS_NAME = "org.mule.api.callback.SourceCallback";

    private Modules() {
    }

    private static Object capability(final String name) {
        return Reflections.invoke(Modules.CAPABILITY_CLASS_NAME, "valueOf", name);
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