package org.mule.tools.module.invocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tools.module.helper.Converters;
import org.mule.tools.module.model.Connector;
import org.mule.tools.module.model.Module;

public class ModuleInvoker {

    private static final Logger LOGGER = Logger.getLogger(ModuleInvoker.class.getPackage().getName());

    private final Module module;
    private final int retryMax;
    private static final int DEFAULT_RETRY_MAX = 5;
    private final Map<Module.Parameter, Object> parameterValues;
    private final Map<Class<?>, Invoker> invokerCache = new HashMap<Class<?>, Invoker>();

    public ModuleInvoker(final Module module) {
        this(module, Collections.<Module.Parameter, Object>emptyMap());
    }

    public ModuleInvoker(final Module module, final Map<Module.Parameter, Object> parameterValues) {
        this(module, parameterValues, ModuleInvoker.DEFAULT_RETRY_MAX);
    }

    public ModuleInvoker(final Module module, final Map<Module.Parameter, Object> overridenParameterValues, final int retryMax) {
        if (module == null) {
            throw new IllegalArgumentException("null module");
        }
        if (overridenParameterValues == null) {
            throw new IllegalArgumentException("null parameterValues");
        }
        if (retryMax <= 0) {
            throw new IllegalArgumentException("retryMax must be > 0");
        }

        //Ensure all mandatory parameter values are provided.
        for (final Module.Parameter parameter : module.getParameters()) {
            if (!parameter.isOptional() && parameter.getDefaultValue() == null
                && !overridenParameterValues.containsKey(parameter)) {
                throw new IllegalArgumentException("Parameter <"+parameter.getName()+"> value must be provided");
            }
        }

        //Aggregate all parameter values: default and overriden ones.
        //Overriden values take precedence over default ones.
        final Map<Module.Parameter, Object> allParameterValues = new HashMap<Module.Parameter, Object>(overridenParameterValues);
        for (final Module.Parameter parameter : module.getParameters()) {
            if (!allParameterValues.containsKey(parameter) && (parameter.getDefaultValue() != null)) {
                allParameterValues.put(parameter, Converters.convert(parameter.getType(), parameter.getDefaultValue()));
            }
        }

        this.module = module;
        this.retryMax = retryMax;
        this.parameterValues = allParameterValues;
    }

    public final Object invoke(final String processorName, final Object message) throws InitialisationException, MuleException {
        if (processorName == null) {
            throw new IllegalArgumentException("null processorName");
        }
        final MessageProcessor messageProcessor = findMessageProcessor(processorName);
        if (messageProcessor == null) {
            throw new IllegalArgumentException("Cannot find a Processor named <"+processorName+">");
        }
        return getInvoker(messageProcessor).invoke(message);
    }

    public final Object invoke(final MessageProcessor messageProcessor, final Object message) throws InitialisationException, MuleException {
        if (messageProcessor == null) {
            throw new IllegalArgumentException("null messageProcessor");
        }
        return getInvoker(messageProcessor).invoke(message);
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

        final Invoker invoker = createInvoker(messageProcessor);
        this.invokerCache.put(key, invoker);
        invoker.initialise();
        return invoker;
    }

    /**
     * @param messageProcessor
     * @return
     * @throws InitialisationException
     * @throws MuleException 
     */
    protected final Invoker createInvoker(final MessageProcessor messageProcessor) {
        final Invoker invoker;
        if (this.module instanceof Connector) {
            invoker = new Invoker(messageProcessor, this.module.getModuleObject(), this.parameterValues, Connector.class.cast(this.module).getConnectionManager(), this.retryMax);
        } else {
            invoker = new Invoker(messageProcessor, this.module.getModuleObject(), this.parameterValues, this.retryMax);
        }
        return invoker;
    }

    /**
     * @param processorName
     * @return {@link MessageProcessor} extracted from {@link Module$Processor}with specified name, null otherwise
     */
    protected final MessageProcessor findMessageProcessor(final String processorName) {
        for (final Module.Processor processor : this.module.getProcessors()) {
            if (processorName.equals(processor.getName())) {
                return processor.getMessageProcessor();
            }
        }
        return null;
    }

    /**
     * Calls {@link Invoker#close()} for all cached {@link Invoker}.
     */
    public final void close() {
        for (final Invoker invoker : this.invokerCache.values()) {
            try {
                invoker.close();
            } catch (MuleException e) {
                if (ModuleInvoker.LOGGER.isLoggable(Level.WARNING)) {
                    ModuleInvoker.LOGGER.log(Level.WARNING, "Got exception while closing <"+invoker+">", e);
                }
            }
        }
    }

}