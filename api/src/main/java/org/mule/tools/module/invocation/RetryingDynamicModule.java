package org.mule.tools.module.invocation;

import java.util.Map;

import java.util.concurrent.atomic.AtomicReference;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.policies.AbstractPolicyTemplate;
import org.mule.tools.module.model.Module;

/**
 * {@link DynamicModule} specialization relying on a {@link RetryPolicyTemplate} to implement retry capacity.
 */
public class RetryingDynamicModule extends DynamicModule {

    private final AbstractPolicyTemplate retryPolicyTemplate;

    public RetryingDynamicModule(final Module module, final Map<String, Object> overriddenParameters, final AbstractPolicyTemplate retryPolicyTemplate) {
        this(module, overriddenParameters, DynamicModule.DEFAULT_RETRY_MAX, retryPolicyTemplate);
    }

    public RetryingDynamicModule(final Module module, final Map<String, Object> overriddenParameters, final int retryMax, final AbstractPolicyTemplate retryPolicyTemplate) {
        super(module, overriddenParameters, retryMax);

        if (retryPolicyTemplate == null) {
            throw new IllegalArgumentException("null retryPolicyTemplate");
        }
        retryPolicyTemplate.setMuleContext(getMuleContext());
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    @Override
    protected <T> T invoke(final MessageProcessor messageProcessor, final Map<String, Object> parameters) throws InitialisationException, MuleException {
        //Force underlying Invoker initialsation. Ensure no InitialisationException won't be thrown in retry loop.
        getInvoker(messageProcessor);

        try {
            final AtomicReference<T> result = new AtomicReference<T>();
            final RetryContext retryContext = this.retryPolicyTemplate.execute(new RetryCallback() {
                @Override
                public void doWork(final RetryContext context) throws Exception {
                    result.set((T) RetryingDynamicModule.super.invoke(messageProcessor, parameters));
                }
                @Override
                public String getWorkDescription() {
                    return "RetryingDynamicModule";
                }
            }, null);
            if (!retryContext.isOk()) {
                throw new RuntimeException(retryContext.getLastFailure());
            }
            return result.get();
        } catch (InitialisationException e) {
            throw e;
        } catch (MuleException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}