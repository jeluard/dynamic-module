package org.mule.tools.module.invocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tools.module.helper.Converters;
import org.mule.tools.module.helper.Parameters;
import org.mule.tools.module.model.Connector;
import org.mule.tools.module.model.Module;

public class ModuleInvoker {

    private static final Logger LOGGER = Logger.getLogger(ModuleInvoker.class.getPackage().getName());

    private final Module module;
    private final int retryMax;
    private static final int DEFAULT_RETRY_MAX = 5;
    private final Map<String, Object> parameterValues;
    private final Map<Class<?>, Invoker> invokerCache = new HashMap<Class<?>, Invoker>();

    public ModuleInvoker(final Module module) {
        this(module, Collections.<String, Object>emptyMap());
    }

    public ModuleInvoker(final Module module, final Map<String, Object> parameterValues) {
        this(module, parameterValues, ModuleInvoker.DEFAULT_RETRY_MAX);
    }

    public ModuleInvoker(final Module module, final Map<String, Object> overridenParameterValues, final int retryMax) {
        if (module == null) {
            throw new IllegalArgumentException("null module");
        }
        if (overridenParameterValues == null) {
            throw new IllegalArgumentException("null parameterValues");
        }
        if (retryMax <= 0) {
            throw new IllegalArgumentException("retryMax must be > 0");
        }

        validateParameterTypeCorrectness(module.getParameters(), overridenParameterValues);
        ensureNoMissingParameterValues(module.getParameters(), overridenParameterValues);

        this.module = module;
        this.retryMax = retryMax;
        this.parameterValues = allParameterValues(module.getParameters(), overridenParameterValues);
    }

    protected final void validateParameterTypeCorrectness(final List<Module.Parameter> defaultParameters, final Map<String, Object> overridenParameterValues) {
        final List<String> incorrectParameterTypes = new LinkedList<String>();
        //Ensure all overriden parameter types are correct.
        for (final Map.Entry<String, Object> entry : overridenParameterValues.entrySet()) {
            final String parameterName = entry.getKey();
            final Module.Parameter parameter = Parameters.getParameter(defaultParameters, parameterName);
            if (parameter == null) {
                if (ModuleInvoker.LOGGER.isLoggable(Level.WARNING)) {
                    ModuleInvoker.LOGGER.log(Level.WARNING, "Value has been provided for unknown parameter <{0}>; it will be ignored", parameterName);
                }

                continue;
            }

            if (!parameter.getType().isAssignableFrom(entry.getValue().getClass())) {
                incorrectParameterTypes.add(parameterName);
            }
        }
        if (!incorrectParameterTypes.isEmpty()) {
            final String terminaison = incorrectParameterTypes.size()>1?"s":"";
            throw new IllegalArgumentException("Incorrect type"+terminaison+" for parameter"+terminaison+" <"+incorrectParameterTypes+">");
        }
    }

    protected final void ensureNoMissingParameterValues(final List<Module.Parameter> defaultParameters, final Map<String, Object> overridenParameterValues) {
        final List<String> missingMandatoryParameterValues = new LinkedList<String>();
        //Ensure all mandatory parameter values are provided.
        for (final Module.Parameter parameter : defaultParameters) {
            if (!parameter.isOptional() && parameter.getDefaultValue() == null
                && !overridenParameterValues.containsKey(parameter.getName())) {
                missingMandatoryParameterValues.add(parameter.getName());
            }
        }
        if (!missingMandatoryParameterValues.isEmpty()) {
            final String terminaison = missingMandatoryParameterValues.size()>1?"s":"";
            throw new IllegalArgumentException("Value"+terminaison+" for parameter"+terminaison+" <"+missingMandatoryParameterValues+"> must be provided");
        }
    }

    /**
     * Aggregate all parameter values: default and overridden ones.
     * Overridden values take precedence over default ones.
     * @return 
     */
    protected final Map<String, Object> allParameterValues(final List<Module.Parameter> defaultParameters, final Map<String, Object> overridenParameterValues) {
        final Map<String, Object> allParameterValues = new HashMap<String, Object>(overridenParameterValues);
        for (final Module.Parameter parameter : defaultParameters) {
            if (!allParameterValues.containsKey(parameter.getName()) && (parameter.getDefaultValue() != null)) {
                //TODO rely on Mule Transformers
                allParameterValues.put(parameter.getName(), Converters.convert(parameter.getType(), parameter.getDefaultValue()));
            }
        }
        return allParameterValues;
    }

    public final Object invoke(final String processorName, final Map<String, Object> overridenParameterValues) throws InitialisationException, MuleException {
        if (processorName == null) {
            throw new IllegalArgumentException("null processorName");
        }
        final Module.Processor processor = findProcessor(processorName);
        if (processor == null) {
            throw new IllegalArgumentException("Cannot find a Processor named <"+processorName+">");
        }

        validateParameterTypeCorrectness(processor.getParameters(), overridenParameterValues);
        ensureNoMissingParameterValues(processor.getParameters(), overridenParameterValues);

        return getInvoker(processor.getMessageProcessor()).invoke(allParameterValues(processor.getParameters(), overridenParameterValues));
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
     * @return {@link Module.Processor} extracted from {@link Module$Processor}with specified name, null otherwise
     */
    protected final Module.Processor findProcessor(final String processorName) {
        for (final Module.Processor processor : this.module.getProcessors()) {
            if (processorName.equals(processor.getName())) {
                return processor;
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