package org.mule.tools.module.helper;

/**
 * Helper methods for {@link ConnectionManager}.
 */
public final class ConnectionManagers {

    private static final String SET_USERNAME_METHOD_NAME = Reflections.setterMethodName("username");
    private static final String SET_PASSWORD_METHOD_NAME = Reflections.setterMethodName("password");
    private static final String SET_SECURITY_TOKEN_METHOD_NAME = Reflections.setterMethodName("securityToken");

    private ConnectionManagers() {
    }

    public static void setUsername(final Object connectionManager, final String username) {
        Reflections.invoke(connectionManager, ConnectionManagers.SET_USERNAME_METHOD_NAME, username);
    }

    public static void setPassword(final Object connectionManager, final String password) {
        Reflections.invoke(connectionManager, ConnectionManagers.SET_PASSWORD_METHOD_NAME, password);
    }

    public static void setSecurityToken(final Object connectionManager, final String securityToken) {
        Reflections.invoke(connectionManager, ConnectionManagers.SET_SECURITY_TOKEN_METHOD_NAME, securityToken);
    }

}