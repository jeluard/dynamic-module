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

package org.mule.tools.module.loader;

import com.google.common.base.CaseFormat;
import java.lang.Package;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.mule.tools.module.helper.*;
import org.mule.tools.module.model.*;

public class Devkit43Loader implements Loader {
//Add support for friendlyName
    private static final String MESSAGE_PROCESSOR_CLASS_SUFFIX = "MessageProcessor";
    private static final String MESSAGE_SOURCE_CLASS_SUFFIX = "MessageSource";
    private static final String TRANSFORMER_CLASS_SUFFIX = "Transformer";
    private static final String PARAMETER_TYPE_FIELD_PREFIX = "_";
    private static final String PARAMETER_TYPE_FIELD_SUFFIX = "Type";
    private static final Set<String> TECHNICAL_FIELD_NAME = new HashSet<String>(Arrays.asList("username", "password", "securityToken", "accessKey", "secretKey"));

    protected final String findMessageProcessorClassName(final Package modulePackage, final String processorName) {
        return modulePackage.getName().replaceAll(".adapters", ".processors")+"."+Strings.capitalize(processorName)+Devkit43Loader.MESSAGE_PROCESSOR_CLASS_SUFFIX;
    }

    protected final String findMessageSourceClassName(final Package modulePackage, final String sourceName) {
        return modulePackage.getName().replaceAll(".adapters", ".sources")+"."+Strings.capitalize(sourceName)+Devkit43Loader.MESSAGE_SOURCE_CLASS_SUFFIX;
    }

    protected final String findTransformerClassName(final Package modulePackage, final String transformerName) {
        return modulePackage.getName()+"."+Strings.capitalize(transformerName)+Devkit43Loader.TRANSFORMER_CLASS_SUFFIX;
    }

    public final Module load(final Class<?> moduleClass, final String connectionManagerClassName) {
        return load(moduleClass, connectionManagerClassName, moduleClass.getPackage());
    }

    public final Module load(final Class<?> moduleClass, final String connectionManagerClassName, final Package modulePackage) {
        if (moduleClass == null) {
            throw new IllegalArgumentException("null moduleClass");
        }

        final Object annotation = Annotations.getConnectorAnnotation(moduleClass);
        if (annotation == null) {
            throw new IllegalArgumentException("Failed to find a Module annotation on <"+moduleClass.getCanonicalName()+">");
        }

        final String name = extractAnnotationName(annotation);
        final String minMuleVersion = extractMinMuleVersion(annotation);
        final List<Parameter> parameters = listParameters(moduleClass);
        final List<Processor> processors = listProcessors(modulePackage, moduleClass);
        final List<Source> sources = listSources(modulePackage, moduleClass);
        final List<Transformer> transformers = listTransformers(modulePackage, moduleClass);
        return new Module(name, minMuleVersion, moduleClass.getName(), parameters, processors, sources, transformers, connectionManagerClassName);
    }

    protected final String extractAnnotationName(final Object annotation) {
        return Reflections.invoke(annotation, "name");
    }

    protected final String extractAnnotationFriendlyName(final Object annotation) {
        return Reflections.invoke(annotation, "friendlyName");
    }

    protected final String extractMinMuleVersion(final Object annotation) {
        return Reflections.invoke(annotation, "minMuleVersion");
    }
    
    protected final List<Parameter> listParameters(final Class<?> moduleClass) {
        final List<Parameter> parameters = new LinkedList<Parameter>();
        for (final Field field : Classes.allDeclaredFields(moduleClass)) {
            if (Annotations.getAnnotation(field, Annotations.CONFIGURABLE_ANNOTATION_CLASS_NAME) != null) {
                final boolean optional = Annotations.getAnnotation(field, Annotations.OPTIONAL_ANNOTATION_CLASS_NAME) != null;
                final String defaultValue = Annotations.getDefaultAnnotationValue(field);
                parameters.add(new Parameter(field.getName(), field.getType(), optional, defaultValue));
            }
        }
        return parameters;
    }

    protected final String extractName(final Object annotation, final Method method) {
        final String annotationName = extractAnnotationName(annotation);
        if (!"".equals(annotationName)) {
            return annotationName;
        }

        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, method.getName());
    }

    protected final String[] extractMethodParameterNames(final Class<?> generatedClass) {
        final List<String> parameterNames = new LinkedList<String>();
        for (final Field field : generatedClass.getDeclaredFields()) {
            final String fieldName = field.getName();
            if (!(fieldName.startsWith(Devkit43Loader.PARAMETER_TYPE_FIELD_PREFIX) && fieldName.endsWith(Devkit43Loader.PARAMETER_TYPE_FIELD_SUFFIX))) {
                continue;
            }

            final String parameterName = Strings.uncapitalize(fieldName.substring(Devkit43Loader.PARAMETER_TYPE_FIELD_PREFIX.length(), fieldName.length()-Devkit43Loader.PARAMETER_TYPE_FIELD_SUFFIX.length()));
            if (Devkit43Loader.TECHNICAL_FIELD_NAME.contains(parameterName)) {
                //Filter fields added by DevKit.
                //TODO What if user parameter have same name?
                continue;
            }

            parameterNames.add(parameterName);
        }
        return parameterNames.toArray(new String[parameterNames.size()]);
    }

    protected final Class<?>[] extractMethodParameterTypes(final Method method) {
        final List<Class<?>> parameterTypes = new LinkedList<Class<?>>();
        for (final Class<?> type : method.getParameterTypes()) {
            //SourceCallback is not a user parameter.
            if (Modules.SOURCE_CALLBACK_CLASS_NAME.equals(type.getName())) {
                continue;
            }

            parameterTypes.add(type);
        }
        return parameterTypes.toArray(new Class<?>[parameterTypes.size()]);
    }

    protected final List<Parameter> listMethodParameters(final Class<?> moduleClass, final Method method, final String generatedClassName) {
        final List<Parameter> parameters = new LinkedList<Parameter>();
        //Rely on the fact that parameters are added first in generated MessageProcessor/MessageSource.
        //TODO Pretty fragile. Replace with stronger alternative.
        final Class<?> generatedClass = Classes.loadClass(generatedClassName);
        if (generatedClass == null) {
            throw new IllegalArgumentException("Failed to load <"+generatedClassName+">");
        }
        final String[] parameterNames = extractMethodParameterNames(generatedClass);
        final Class<?>[] parameterTypes = extractMethodParameterTypes(method);
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterTypes.length < parameterNames.length || parameterAnnotations.length < parameterNames.length) {
            throw new IllegalArgumentException("Failed to match method <"+method.getName()+"> parameters:\nnames <"+Arrays.toString(parameterNames) +">\ntypes <"+Arrays.toString(parameterTypes) +">\nannotations <"+Arrays.deepToString(parameterAnnotations) +">");
        }
        for (int i = 0; i < parameterNames.length; i++) {
            final String name = parameterNames[i];
            final Class<?> type = parameterTypes[i];
            final List<Annotation> annotations = Arrays.asList(parameterAnnotations[i]);
            boolean optional = false;
            String defaultValue = null;
            for (final Annotation annotation : annotations) {
                if (Annotations.OPTIONAL_ANNOTATION_CLASS_NAME.equals(annotation.annotationType().getName())) {
                    optional = true;
                }
                if (Annotations.DEFAULT_ANNOTATION_CLASS_NAME.equals(annotation.annotationType().getName())) {
                    defaultValue = Reflections.invoke(annotation, "value");
                }
            }

            parameters.add(new Parameter(name, type, optional, defaultValue));
        }
        return parameters;
    }

    protected final List<Processor> listProcessors(final Package modulePackage, final Class<?> moduleClass) {
        final List<Processor> processors = new LinkedList<Processor>();
        for (final Method method : moduleClass.getMethods()) {
            final Object annotation = Annotations.getAnnotation(method, Annotations.PROCESSOR_ANNOTATION_CLASS_NAME);
            if (annotation != null) {
                final String messageProcessorClassName = findMessageProcessorClassName(modulePackage, method.getName());
                processors.add(new Processor(extractName(annotation, method), extractAnnotationFriendlyName(annotation), messageProcessorClassName, listMethodParameters(moduleClass, method, messageProcessorClassName), method.getReturnType().getName(), Reflections.<Boolean>invoke(annotation, "intercepting")));
            }
        }
        return processors;
    }

    protected final List<Source> listSources(final Package modulePackage, final Class<?> moduleClass) {
        final List<Source> sources = new LinkedList<Source>();
        for (final Method method : moduleClass.getMethods()) {
            final Object annotation = Annotations.getAnnotation(method, Annotations.SOURCE_ANNOTATION_CLASS_NAME);
            if (annotation != null) {
                final String messageSourceClassName = findMessageSourceClassName(modulePackage, method.getName());
                sources.add(new Source(extractName(annotation, method), extractAnnotationFriendlyName(annotation), messageSourceClassName, listMethodParameters(moduleClass, method, messageSourceClassName)));
            }
        }
        return sources;
    }

    protected final List<Transformer> listTransformers(final Package modulePackage, final Class<?> moduleClass) {
        final List<Transformer> transformers = new LinkedList<Transformer>();
        for (final Method method : moduleClass.getMethods()) {
            final Object annotation = Annotations.getAnnotation(method, Annotations.TRANSFORMER_ANNOTATION_CLASS_NAME);
            if (annotation != null) {
                final String transformerClassName = findTransformerClassName(modulePackage, method.getName());
                transformers.add(new Transformer(transformerClassName, Reflections.<Integer>invoke(annotation, "priorityWeighting"), Reflections.<Class[]>invoke(annotation, "sourceTypes")));
            }
        }
        return transformers;
    }

}