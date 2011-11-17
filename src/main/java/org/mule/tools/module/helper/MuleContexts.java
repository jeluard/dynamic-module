package org.mule.tools.module.helper;

import java.util.ArrayList;
import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;

/**
 *Helper methods for {@link MuleContext}.
 */
public final class MuleContexts {

    private MuleContexts() {
    }

    /**
     * @return a default {@link MuleContext}
     * @throws InitialisationException
     * @throws ConfigurationException 
     */
    public static MuleContext defaultMuleContext() throws InitialisationException, ConfigurationException {
        final MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        final List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        builders.add(new SimpleConfigurationBuilder(null));
        return muleContextFactory.createMuleContext(builders, new DefaultMuleContextBuilder());
    }

}