package org.mule.tools.module.helper;

import java.util.ArrayList;
import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tools.module.transformer.StringToURL;

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
    public static MuleContext defaultMuleContext() throws InitialisationException, ConfigurationException, MuleException {
        final MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        final List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        builders.add(new SimpleConfigurationBuilder(null));
        final MuleContext context = muleContextFactory.createMuleContext(builders, new DefaultMuleContextBuilder());
        //Register all default stuff
        final SimpleRegistryBootstrap bootstrap = new SimpleRegistryBootstrap();
        bootstrap.setMuleContext(context);
        bootstrap.initialise();
        context.getRegistry().registerTransformer(new StringToURL());
        return context;
    }

    public static void inject(final Object object, final MuleContext context) {
        if (object instanceof MuleContextAware) {
            MuleContextAware.class.cast(object).setMuleContext(context);
        }
    }

}