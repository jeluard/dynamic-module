package org.mule.tools.module.helper;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.session.DefaultMuleSession;

/**
 * Helper methods for {@link MuleEvent}.
 */
public class MuleEvents {

    /**
     * @param message
     * @param context
     * @return a default {@link MuleEvent}
     */
    public static MuleEvent defaultMuleEvent(final Object message, final MuleContext context) {
        return new DefaultMuleEvent(new DefaultMuleMessage(message, context), MessageExchangePattern.REQUEST_RESPONSE, new DefaultMuleSession(context));
    }

}