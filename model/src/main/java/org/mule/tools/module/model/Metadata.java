package org.mule.tools.module.model;

import com.google.common.base.Preconditions;

import java.net.URL;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Metadata {

    public enum Icon {
        CLOUD_CONNECTOR_SMALL,
        CLOUD_CONNECTOR_LARGE,
        TRANSFORMER_SMALL,
        TRANSFORMER_LARGE,
        ENDPOINT_SMALL,
        ENDPOINT_LARGE
    }

    private final URL homepage;
    private final Map<Icon, URL> icons;

    public Metadata(final URL homepage, final Map<Icon, URL> icons) {
        Preconditions.checkNotNull(homepage, "null homepage");
        Preconditions.checkNotNull(icons, "null icons");

        this.homepage = homepage;
        this.icons = Collections.unmodifiableMap(new EnumMap<Icon, URL>(icons));
    }

    public final URL getHomepage() {
        return this.homepage;
    }

    public final Map<Icon, URL> getIcons() {
        return this.icons;
    }

}