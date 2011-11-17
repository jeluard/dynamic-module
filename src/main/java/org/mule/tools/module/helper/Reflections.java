package org.mule.tools.module.helper;

import org.mule.util.StringUtils;

/**
 * Helper methods for reflection.
 */
public final class Reflections {

    private Reflections() {
    }

    /**
     * @param propertyName
     * @return default setter name for specified property
     */
    public static String setterMethodName(final String propertyName) {
        return "set"+StringUtils.capitalize(propertyName);
    }

    /**
     * Sets property to value for specified object.
     * @param propertyName
     * @param object
     * @param value 
     */
    public static void set(final Object object, final String propertyName, final Object value) {
        Reflections.invoke(object, Reflections.setterMethodName(propertyName), value);
    }

    private static Class<?> asPrimitiveType(final Class<?> type) {
        if (type.equals(Integer.class)) {
            return int.class;
        } else if (type.equals(Float.class)) {
            return float.class;
        } else if (type.equals(Long.class)) {
            return long.class;
        } else if (type.equals(Double.class)) {
            return double.class;
        } else if (type.equals(Character.class)) {
            return char.class;
        } else if (type.equals(Byte.class)) {
            return byte.class;
        } else if (type.equals(Short.class)) {
            return short.class;
        } else {
            return null;
        }
    }

    /**
     * @param <T>
     * @param method
     * @param object
     * @param argument
     * @return result of dynamic invocation of `method` on `object` with `argument`.
     * @see #asTypes(java.lang.Object[]) for inferred type from arguments
     */
    public static <T> T invoke(final Object object, final String method, final Object argument) {
        try {
            return Reflections.invoke(object, method, argument, argument.getClass());
        } catch (RuntimeException e) {
            final Class<?> primitiveType = asPrimitiveType(argument.getClass());
            if (primitiveType != null) {
                return Reflections.invoke(object, method, argument, primitiveType);
            }

            throw e;
        }
    }

    /**
     * @param <T>
     * @param method
     * @param object
     * @param argument
     * @param argumentType 
     * @return result of dynamic invocation of `method` on `object` with `argument`.
     */
    public static <T> T invoke(final Object object, final String method, final Object argument, final Class<?> argumentType) {
        try {
            return (T) object.getClass().getMethod(method, argumentType).invoke(object, argument);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke <"+method+"> with arguments <"+argument+"> on <"+object+">", e);
        }
    }

}