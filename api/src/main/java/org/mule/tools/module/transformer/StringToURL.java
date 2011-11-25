package org.mule.tools.module.transformer;

import java.net.MalformedURLException;
import java.net.URL;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

public class StringToURL extends AbstractTransformer implements DiscoverableTransformer {

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public StringToURL() {
        registerSourceType(new SimpleDataType<Object>(String.class));
        setReturnDataType(DataTypeFactory.create(URL.class));
    }

    @Override
    protected final Object doTransform(final Object source, final String encoding) throws TransformerException {
        try {
            return new URL(source.toString());
        } catch (MalformedURLException e) {
            throw new TransformerException(CoreMessages.createStaticMessage("Unable to transform <"+source+"> to a "+URL.class.getSimpleName()), e);
        }
    }

    @Override
    public final int getPriorityWeighting() {
        return this.priorityWeighting;
    }

    @Override
    public final void setPriorityWeighting(final int priorityWeighting) {
        this.priorityWeighting = priorityWeighting;
    }
    
}