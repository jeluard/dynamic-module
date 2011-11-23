package org.mule.tools.module.invocation;

import java.util.Map;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tools.module.helper.LifeCycles;
import org.mule.tools.module.helper.MuleEvents;
import org.mule.tools.module.helper.Reflections;

public class Invoker implements Disposable {

    private final MuleContext context;
    private final MessageProcessor messageProcessor;
    private final int retryMax;
    private static final String RETRY_MAX_FIELD_NAME = "retryMax";

    public Invoker(final MuleContext context, final MessageProcessor messageProcessor, final int retryMax) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        if (messageProcessor == null) {
            throw new IllegalArgumentException("null messageProcessor");
        }

        this.context = context;
        this.messageProcessor = messageProcessor;
        this.retryMax = retryMax;

        try {
            initialise();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initialise() throws InitialisationException, MuleException {
        Reflections.set(this.messageProcessor, Invoker.RETRY_MAX_FIELD_NAME, this.retryMax);

        MuleContextAware.class.cast(this.messageProcessor).setMuleContext(this.context);
        LifeCycles.initialise(this.messageProcessor);
        LifeCycles.start(this.messageProcessor);
    }

    public final <T> T invoke(final Map<String, Object> processorParameters) throws MuleException {
        if (processorParameters == null) {
            throw new IllegalArgumentException("null processorParameters");
        }

        //Set all parameter values on the MessageProcessor.
        Reflections.set(this.messageProcessor, processorParameters);

        final MuleEvent muleEvent = MuleEvents.defaultMuleEvent(processorParameters, this.context);
        return (T) this.messageProcessor.process(muleEvent).getMessage().getPayload();
    }

    @Override
    public final void dispose() {
        try {
            LifeCycles.stop(this.messageProcessor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LifeCycles.dispose(this.messageProcessor);
    }

}