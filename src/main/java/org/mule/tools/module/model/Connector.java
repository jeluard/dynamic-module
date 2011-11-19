package org.mule.tools.module.model;

import java.util.List;

import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.ConnectionManager;
import org.mule.tools.module.helper.ConnectionManagers;

/**
 * @see org.mule.api.annotations.Connector
 * 
 * TODO Abstract connection management stuff, pull it in Module, suppress this.
 */
public class Connector extends Module {

    private final ConnectionManager<?, ?> connectionManager;

    public Connector(final String name, final Object module, final Capabilities capabilities, final List<Module.Parameter> parameters, final List<Module.Processor> processors, final ClassLoader classLoader) {
        super(name, module, capabilities, parameters, processors, classLoader);

        this.connectionManager = null;
    }

    public Connector(final String name, final Object module, final Capabilities capabilities, final List<Module.Parameter> parameters, final List<Module.Processor> processors, final ConnectionManager<?, ?> connectionManager, final ClassLoader classLoader) {
        super(name, module, capabilities, parameters, processors, classLoader);

        if (connectionManager == null) {
            throw new IllegalArgumentException("null connectionManager");
        }
        if (!capabilities.isCapableOf(Capability.CONNECTION_MANAGEMENT_CAPABLE)) {
            throw new IllegalArgumentException(Capabilities.class.getSimpleName()+" does not support "+Capability.CONNECTION_MANAGEMENT_CAPABLE);
        }

        this.connectionManager = connectionManager;
    }

    @Override
    public Object getModuleObject() {
        if (getConnectionManager() != null) {
            return getConnectionManager();
        }
        return super.getModuleObject();
    }

    public final ConnectionManager<?, ?> getConnectionManager() {
        return this.connectionManager;
    }

    public final void setUsername(final String username) {
        ConnectionManagers.setUsername(this.connectionManager, username);
    }

    public final void setPassword(final String password) {
        ConnectionManagers.setPassword(this.connectionManager, password);
    }

    public final void setSecurityToken(final String securityToken) {
        ConnectionManagers.setSecurityToken(this.connectionManager, securityToken);
    }

}