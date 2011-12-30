package org.mule.tools.module.helper;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class Annotations {

    public static final String CONNECTOR_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.Connector";
    public static final String MODULE_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.Module";
    public static final String CONFIGURABLE_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.Configurable";
    public static final String OPTIONAL_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.param.Optional";
    public static final String DEFAULT_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.param.Default";
    public static final String PROCESSOR_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.Processor";
    public static final String SOURCE_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.Source";
    public static final String TRANSFORMER_ANNOTATION_CLASS_NAME = "org.mule.api.annotations.Transformer";

    private Annotations() {
    }

    private static Object getAnnotation(final List<Annotation> annotations, final String annotationName) {
        for (final Annotation annotation : annotations) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return annotation;
            }
        }
        return null;
    }

    private static Object getAnnotation(final Class<?> type, final String annotationName) {
        return Annotations.getAnnotation(Classes.allAnnotations(type), annotationName);
    }

    public static Object getAnnotation(final AnnotatedElement element, final String annotationName) {
        return Annotations.getAnnotation(Arrays.asList(element.getAnnotations()), annotationName);
    }

    public static Object getConnectorAnnotation(final Class<?> type) {
        final Object connectorAnnotation = Annotations.getAnnotation(type, Annotations.CONNECTOR_ANNOTATION_CLASS_NAME);
        if (connectorAnnotation != null) {
            return connectorAnnotation;
        }
        return Annotations.getAnnotation(type, Annotations.MODULE_ANNOTATION_CLASS_NAME);
    }

    public static String getDefaultAnnotationValue(final Field field) {
        final Object defaultAnnotation = Annotations.getAnnotation(field, Annotations.DEFAULT_ANNOTATION_CLASS_NAME);
        if (defaultAnnotation == null) {
            return null;
        }
        return Reflections.invoke(defaultAnnotation, "value");
    }

}