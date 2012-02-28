package org.mule.tools.module.model;

import com.google.common.base.Preconditions;

public class Package {

    private final Module module;
    private final Metadata metadata;

    public Package(final Module module, final Metadata metadata) {
        Preconditions.checkNotNull(module, "null module");
        Preconditions.checkNotNull(metadata, "null metadata");

        this.module = module;
        this.metadata = metadata;
    }

    public final Module getModule() {
        return this.module;
    }

    public final Metadata getMetadata() {
        return this.metadata;
    }

}