package org.mule.tools.module.helper;

import java.util.LinkedList;
import java.util.List;

/**
 * Helper methods for {@link Class}.
 */
public final class Classes {

    private Classes() {
    }

    /**
     * @param clazz
     * @return all subclasses of specified {@link Class}
     */
    public static List<Class<?>> allSuperClasses(final Class<?> clazz) {
        final List<Class<?>> allSuperClasses = new LinkedList<Class<?>>();
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
            allSuperClasses.add(superClass);
            superClass = superClass.getSuperclass();
        }
        return allSuperClasses;
    }

}