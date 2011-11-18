package org.mule.tools.module.helper;

import java.lang.reflect.Field;
import org.mule.util.StringUtils;

/**
 * Helper methods for reflection.
 */
public final class Reflections {

    private Reflections() {
    }

    public static Field setAccessible(final Object object, final String propertyName) {
        try {
            final Field field = object.getClass().getDeclaredField(propertyName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to make <"+propertyName+"> accessible", e);
        }
    }

    /**
     * @param propertyName
     * @return default getter name for specified property
     */
    public static String getterMethodName(final String propertyName) {
        return "get"+StringUtils.capitalize(propertyName);
    }

    /**
     * Get value of property for specified object.
     * @param propertyName
     * @param object
     */
    public static Object get(final Object object, final String propertyName) {
        try {
            return Reflections.invoke(object, Reflections.getterMethodName(propertyName), void.class);
        } catch (RuntimeException e) {
            final Field field = Reflections.setAccessible(object, propertyName);
            try {
                return field.get(object);
            } catch(IllegalAccessException ee) {
                throw new RuntimeException(ee);
            }
        }
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
        try {
            Reflections.invoke(object, Reflections.setterMethodName(propertyName), value);
        } catch (RuntimeException e) {
            final Field field = Reflections.setAccessible(object, propertyName);
            try {
                field.set(object, value);
            } catch(IllegalAccessException ee) {
                throw new RuntimeException(ee);
            }
        }
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