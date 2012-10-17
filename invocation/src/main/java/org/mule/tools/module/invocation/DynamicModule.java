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

package org.mule.tools.module.invocation;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.source.MessageSource;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tools.module.helper.*;
import org.mule.tools.module.model.Module;
import org.mule.tools.module.model.Parameter;
import org.mule.tools.module.model.Processor;
import org.mule.tools.module.model.Source;
import org.mule.transformer.types.DataTypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicModule implements Disposable {

    /**
     * Encapsulate logic dealing with event received from a {@link Source}.
     */
    public interface Listener {

        /**
         * Called every time associated {@link Source} fires an event.
         */
        void onEvent(MuleEvent message);

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicModule.class.getPackage().getName());

    private final MuleContext context;
    private final ClassLoader classLoader;
    private final Module module;
    private Object moduleObject;
    private Object connectionManager;
    private static final String MODULE_OBJECT_REGISTRY_KEY = "moduleObject";
    private final int retryMax;
    protected static final int DEFAULT_RETRY_MAX = 5;
    private final Map<String, Object> parameters;
    private final Map<String, Object> connectionParameters;
    private final Map<String, MessageProcessor> messageProcessorCache = new HashMap<String, MessageProcessor>();
    private final Map<Class<?>, Invoker> invokerCache = new HashMap<Class<?>, Invoker>();
    private final Map<Class<?>, Registrar> registrarCache = new HashMap<Class<?>, Registrar>();

    //TODO Introduce builder
    public DynamicModule(final List<URL> urls, final Module module) {
        this(new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader()), module);
    }

    public DynamicModule(final ClassLoader classLoader, final Module module) {
        this(classLoader, module, Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap());
    }

    public DynamicModule(final List<URL> urls, final Module module, final Map<String, Object> overriddenParameters, final Map<String, Object> connectionParameters) {
        this(new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader()), module, overriddenParameters, connectionParameters);
    }

    public DynamicModule(final ClassLoader classLoader, final Module module, final Map<String, Object> overriddenParameters, final Map<String, Object> connectionParameters) {
        this(classLoader, module, overriddenParameters, connectionParameters, DynamicModule.DEFAULT_RETRY_MAX);
    }

    public DynamicModule(final ClassLoader classLoader, final Module module, final Map<String, Object> overriddenParameters, final Map<String, Object> overriddenConnectionParameters, final int retryMax) {
        if (classLoader == null) {
            throw new IllegalArgumentException("null classLoader");
        }
        if (module == null) {
            throw new IllegalArgumentException("null module");
        }
        if (overriddenParameters == null) {
            throw new IllegalArgumentException("null overriddenParameters");
        }
        if (overriddenConnectionParameters == null) {
            throw new IllegalArgumentException("null overriddenConnectionParameters");
        }
        if (retryMax <= 0) {
            throw new IllegalArgumentException("retryMax must be > 0");
        }

        validateParameterTypeCorrectness(module.getParameters(), overriddenParameters);
        ensureNoMissingParameters(module.getParameters(), overriddenParameters);

        try {
            this.context = MuleContexts.defaultMuleContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.classLoader = classLoader;
        this.module = module;
        this.retryMax = retryMax;
        this.parameters = allParameters(module.getParameters(), overriddenParameters);
        this.connectionParameters = overriddenConnectionParameters;//TODO add support for default values
        
        try {
            initialise();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final MuleContext getMuleContext() {
        return this.context;
    }

    private void initialise() throws InitialisationException, RegistrationException, MuleException {
        final Class<?> moduleObjectClass = Classes.loadClass(this.classLoader, this.module.getType());
        if (moduleObjectClass == null) {
            throw new IllegalArgumentException("Failed to load <"+this.module.getType()+">");
        }
        this.moduleObject = Classes.newInstance(moduleObjectClass);
        if (Modules.isLifeCycleCapable(this.moduleObject)) {
            LifeCycles.initialise(this.moduleObject);
            LifeCycles.start(this.moduleObject);
        }
        if (this.module.getConnectionManagerType() != null) {
            final Class<?> connectionManagerClass = Classes.loadClass(this.classLoader, this.module.getConnectionManagerType());
            if (connectionManagerClass == null) {
                throw new IllegalArgumentException("Failed to load <"+this.module.getConnectionManagerType()+">");
            }
            this.connectionManager = Classes.newInstance(connectionManagerClass);
            if (Modules.isLifeCycleCapable(this.connectionManager)) {
                LifeCycles.initialise(this.connectionManager);
            }
            for (final Map.Entry<String, Object> entry : this.connectionParameters.entrySet()) {
                Reflections.set(this.connectionManager, entry.getKey(), entry.getValue());
            }
        }

        //Apply parameters to the ModuleObject.
        final Object object = this.connectionManager != null ? this.connectionManager : this.moduleObject;
        for (final Map.Entry<String, Object> entry : this.parameters.entrySet()) {
            Reflections.set(object, entry.getKey(), entry.getValue());
        }

        this.context.getRegistry().registerObject(DynamicModule.MODULE_OBJECT_REGISTRY_KEY, object);
    }

    protected final void validateParameterTypeCorrectness(final List<Parameter> defaultParameters, final Map<String, Object> overriddenParameters) {
        final List<String> incorrectParameterTypes = new LinkedList<String>();
        //Ensure all overridden parameter types are correct.
        for (final Map.Entry<String, Object> entry : overriddenParameters.entrySet()) {
            final String parameterName = entry.getKey();
            final Parameter parameter = Parameters.getParameter(defaultParameters, parameterName);
            if (parameter == null) {
                continue;
            }

            final Class<?> expectedType = Reflections.asType(parameter.getType());
            final Class<?> type = entry.getValue().getClass();
            if (!expectedType.isAssignableFrom(type)) {
                final StringBuilder details = new StringBuilder(parameterName);
                details.append("(type ").append(type.getCanonicalName()).append(" is not assignable to ").append(expectedType.getCanonicalName()).append(")");
                incorrectParameterTypes.add(details.toString());
            }
        }
        if (!incorrectParameterTypes.isEmpty()) {
            final String terminaison = incorrectParameterTypes.size()>1?"s":"";
            throw new IllegalArgumentException("Incorrect type"+terminaison+" for parameter"+terminaison+" <"+incorrectParameterTypes+">");
        }
    }

    protected final void ensureNoMissingParameters(final List<Parameter> defaultParameters, final Map<String, Object> overriddenParameters) {
        final List<String> missingMandatoryParameters = new LinkedList<String>();
        //Ensure all mandatory parameter values are provided.
        for (final Parameter parameter : defaultParameters) {
            if (!parameter.isOptional() && parameter.getDefaultValue() == null
                && !overriddenParameters.containsKey(parameter.getName())) {
                missingMandatoryParameters.add(parameter.getName());
            }
        }
        if (!missingMandatoryParameters.isEmpty()) {
            final String terminaison = missingMandatoryParameters.size()>1?"s":"";
            throw new IllegalArgumentException("Value"+terminaison+" for parameter"+terminaison+" <"+missingMandatoryParameters+"> must be provided");
        }
    }

    /**
     * Aggregate all parameters: default and overridden ones.
     * Overridden parameters take precedence over default ones.
     * @return 
     */
    protected final Map<String, Object> allParameters(final List<Parameter> defaultParameters, final Map<String, Object> overriddenParameters) {
        final Map<String, Object> allParameters = new HashMap<String, Object>();
        final Set<String> defaultParameterNames = new HashSet<String>();
        for (final Parameter parameter : defaultParameters) {
            //Only add default values
            if (parameter.getDefaultValue() != null) {
                try {
                    final Transformer transformer = this.context.getRegistry().lookupTransformer(DataType.STRING_DATA_TYPE, DataTypeFactory.create(parameter.getType()));
                    allParameters.put(parameter.getName(), transformer.transform(parameter.getDefaultValue()));
                } catch (TransformerException e) {
                    throw new RuntimeException("Failed to transform <"+parameter.getDefaultValue()+"> to <"+parameter.getType()+"> for parameter <"+parameter.getName()+">", e);
                }
                
            }
            defaultParameterNames.add(parameter.getName());
        }
        for (final Map.Entry<String, Object> entry : overriddenParameters.entrySet()) {
            //Only add existing parameters
            final String parameterName = entry.getKey();
            if (!defaultParameterNames.contains(parameterName)) {
                if (DynamicModule.LOGGER.isWarnEnabled()) {
                    DynamicModule.LOGGER.warn("Value has been provided for unknown parameter <"+parameterName+">; it will be ignored");
                }

                continue;
            }

            allParameters.put(parameterName, entry.getValue());
        }
        return allParameters;
    }

    /**
     * @param processorName
     * @return {@link Module.Processor} extracted from {@link Module$Processor}with specified name, null otherwise
     */
    protected final Processor findProcessor(final String processorName) {
        for (final Processor processor : this.module.getProcessors()) {
            if (processorName.equals(processor.getName())) {
                return processor;
            }
        }
        return null;
    }

    /**
     * @param messageProcessor
     * @return an {@link Invoker} for {@link MessageProcessor}. Creates it if needed.
     * @throws InitialisationException
     * @throws MuleException
     * @see #createInvoker(org.mule.api.processor.MessageProcessor) 
     */
    protected synchronized final Invoker getInvoker(final MessageProcessor messageProcessor) throws InitialisationException, MuleException {
        final Class<?> key = messageProcessor.getClass();
        if (this.invokerCache.containsKey(key)) {
            return this.invokerCache.get(key);
        }

        final Invoker invoker = new Invoker(this.context, messageProcessor, this.retryMax);
        this.invokerCache.put(key, invoker);
        return invoker;
    }

    /**
     * Invoke `processorName` with provided `overriddenParameters`. Non overridden parameters will rely on default value.
     * @param <T>
     * @param processorName
     * @param overriddenParameters
     * @return
     * @throws InitialisationException
     * @throws MuleException 
     */
    public final <T> T invoke(final String processorName, final Map<String, Object> overriddenParameters) throws InitialisationException, MuleException {
        if (processorName == null) {
            throw new IllegalArgumentException("null processorName");
        }
        if (overriddenParameters == null) {
            throw new IllegalArgumentException("null overriddenParameters");
        }

        final Processor processor = findProcessor(processorName);
        if (processor == null) {
            throw new IllegalArgumentException("Cannot find a Processor named <"+processorName+">");
        }

        validateParameterTypeCorrectness(processor.getParameters(), overriddenParameters);
        ensureNoMissingParameters(processor.getParameters(), overriddenParameters);

        return invoke(getMessageProcessor(processor.getType()), allParameters(processor.getParameters(), overriddenParameters));
    }

    public synchronized final MessageProcessor getMessageProcessor(final String type) {
        if (this.messageProcessorCache.containsKey(type)) {
            return this.messageProcessorCache.get(type);
        } else {
            final Class<MessageProcessor> messageProcessorType = Classes.loadClass(this.classLoader, type);
            if (messageProcessorType == null) {
                throw new IllegalArgumentException("Cannot load <"+type+">");
            }
            final MessageProcessor messageProcessor = Classes.newInstance(messageProcessorType);
            this.messageProcessorCache.put(type, messageProcessor);
            return messageProcessor;
        }
    }

    protected <T> T invoke(final MessageProcessor messageProcessor, final Map<String, Object> parameters) throws InitialisationException, MuleException {
        return getInvoker(messageProcessor).invoke(parameters);
    }

    /**
     * @param sourceName
     * @return {@link Module.Source} extracted from {@link Module$Source}with specified name, null otherwise
     */
    protected final Source findSource(final String sourceName) {
        for (final Source source : this.module.getSources()) {
            if (sourceName.equals(source.getName())) {
                return source;
            }
        }
        return null;
    }

    /**
     * @param messageSourceType
     * @return a cached {@link Invoker} for {@link MessageProcessor}.
     * @throws InitialisationException
     * @throws MuleException
     * @see #createInvoker(org.mule.api.processor.MessageProcessor) 
     */
    protected synchronized final Registrar getRegistrar(final Class<MessageSource> messageSourceType) throws InitialisationException, MuleException {
        return this.registrarCache.get(messageSourceType);
    }

    /**
     * @param messageSource
     * @return a new cached {@link Regsitrar}
     */
    protected synchronized final Registrar createAndCacheRegistrar(final Class<MessageSource> messageSourceType) {
        final Registrar registrar = new Registrar(this.context, Classes.<MessageSource>newInstance(messageSourceType));
        this.registrarCache.put(messageSourceType, registrar);
        return registrar;
    }

    /**
     * Subscribe {@link Listener} to `sourceName` {@link Source} with `overriddenParameters`.
     * @param sourceName
     * @param overriddenParameters
     * @param listener
     * @throws InitialisationException
     * @throws MuleException 
     */
    public synchronized final void subscribe(final String sourceName, final Map<String, Object> overriddenParameters, final Listener listener) throws InitialisationException, MuleException {
        if (sourceName == null) {
            throw new IllegalArgumentException("null sourceName");
        }
        if (overriddenParameters == null) {
            throw new IllegalArgumentException("null overriddenParameters");
        }

        final Source source = findSource(sourceName);
        if (source == null) {
            throw new IllegalArgumentException("Cannot find a Source named <"+sourceName+">");
        }

        validateParameterTypeCorrectness(source.getParameters(), overriddenParameters);
        ensureNoMissingParameters(source.getParameters(), overriddenParameters);

        final Registrar registrar = getRegistrar(Classes.<MessageSource>loadClass(this.classLoader, source.getType()));
        if (registrar != null) {
            throw new IllegalStateException("Source <"+sourceName+"> is already subscribed");
        }
        createAndCacheRegistrar(Classes.<MessageSource>loadClass(this.classLoader, source.getType())).start(allParameters(source.getParameters(), overriddenParameters), listener);
    }

    /**
     * Unsubscribe {@link Listener} previously registered to `sourceName` {@link Source}.
     * @param sourceName
     * @throws InitialisationException
     * @throws MuleException 
     */
    public final void unsubscribe(final String sourceName) throws MuleException {
        if (sourceName == null) {
            throw new IllegalArgumentException("null sourceName");
        }

        final Source source = findSource(sourceName);
        if (source == null) {
            throw new IllegalArgumentException("Cannot find a Source named <"+sourceName+">");
        }

        final Registrar registrar = getRegistrar(Classes.<MessageSource>loadClass(this.classLoader, source.getType()));
        if (registrar == null) {
            throw new IllegalStateException("Source <"+sourceName+"> is not subscribed");
        }
        registrar.stop();
    }

    /**
     * Cleanup all internal resources:
     * * call {@link Invoker#dispose()} for all cached {@link Invoker}
     * * call {@link Registrar#stop()} for all cached {@link Registrar}
     * * call {@link MuleCOntext#dispose()}
     */
    @Override
    public final void dispose() {
        for (final Invoker invoker : this.invokerCache.values()) {
            invoker.dispose();
        }
        this.invokerCache.clear();
        for (final Registrar registrar : this.registrarCache.values()) {
            try {
                registrar.stop();
            } catch (MuleException e) {
                if (DynamicModule.LOGGER.isWarnEnabled()) {
                    DynamicModule.LOGGER.warn("Got exception while closing <"+registrar+">", e);
                }
            }
        }
        this.registrarCache.clear();
        this.messageProcessorCache.clear();
        this.context.dispose();
    }

}