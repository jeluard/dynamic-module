package org.mule.tools.module.invocation;

import java.util.Map;

import org.mule.api.ConnectionManager;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.tools.module.helper.MuleContexts;
import org.mule.tools.module.helper.MuleEvents;
import org.mule.tools.module.helper.Reflections;

//TODO Add support for OAuth1 and OAuth2
public class Invoker {

    private final MessageProcessor messageProcessor;
    private final Object moduleObject;
    private final Map<String, Object> parameterValues;
    private final ConnectionManager<?, ?> connectionManager;
    private final int retryMax;
    private static final String RETRY_MAX_FIELD_NAME = "retryMax";
    private static final String MODULE_OBJECT_REGISTRY_KEY = "moduleObject";
    private MuleContext context;

    public Invoker(final MessageProcessor processor, final Object moduleObject, final Map<String, Object> parameterValues, final int retryMax) {
        this(processor, moduleObject, parameterValues, null, retryMax);
    }

    public Invoker(final MessageProcessor messageProcessor, final Object moduleObject, final Map<String, Object> parameterValues, final ConnectionManager<?, ?> connectionManager, final int retryMax) {
        if (messageProcessor == null) {
            throw new IllegalArgumentException("null messageProcessor");
        }
        if (moduleObject == null) {
            throw new IllegalArgumentException("null moduleObject");
        }
        if (parameterValues == null) {
            throw new IllegalArgumentException("null parameterValues");
        }
        if (connectionManager == null) {
            throw new IllegalArgumentException("null connectionManager");
        }

        this.messageProcessor = messageProcessor;
        this.moduleObject = moduleObject;
        this.parameterValues = parameterValues;
        this.connectionManager = connectionManager;
        this.retryMax = retryMax;
    }

    public final void initialise() throws InitialisationException, MuleException {
        initialise(MuleContexts.defaultMuleContext());
    }

    public final void initialise(final MuleContext context) throws InitialisationException, MuleException {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }

        this.context = context;

        //Initialise moduleObject
        for (final Map.Entry<String, Object> entry : this.parameterValues.entrySet()) {
            Reflections.set(this.moduleObject, entry.getKey(), entry.getValue());
        }
        context.getRegistry().registerObject(Invoker.MODULE_OBJECT_REGISTRY_KEY, this.moduleObject);

        //Initialise MessageProcessor
        Reflections.set(this.messageProcessor, Invoker.RETRY_MAX_FIELD_NAME, this.retryMax);

        MuleContextAware.class.cast(this.messageProcessor).setMuleContext(context);
        Initialisable.class.cast(this.messageProcessor).initialise();
        Startable.class.cast(this.messageProcessor).start();

        //Initialise ConnectionManager
        if (this.connectionManager != null) {
            Initialisable.class.cast(this.connectionManager).initialise();
        }
    }

    public final <T> T invoke(final Map<String, Object> parameterValues) throws MuleException {
        //Set all parameter values on the MessageProcessor.
        for (final Map.Entry<String, Object> entry : parameterValues.entrySet()) {
            final String parameterName = entry.getKey();
            try {
                Reflections.invoke(this.messageProcessor, Reflections.setterMethodName(entry.getKey()), entry.getValue(), Object.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set parameter <"+parameterName+">", e);
            }
        }

        final MuleEvent muleEvent = MuleEvents.defaultMuleEvent(parameterValues, this.context);
        return (T) this.messageProcessor.process(muleEvent).getMessage().getPayload();
    }

    public final void close() throws MuleException {
        Stoppable.class.cast(this.messageProcessor).stop();
        Disposable.class.cast(this.messageProcessor).dispose();
    }

}