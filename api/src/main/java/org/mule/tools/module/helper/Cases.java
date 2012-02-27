/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mule.tools.module.helper;

import java.lang.reflect.Method;

/**
 *
 * @author julien
 */
public class Cases {

    /**
     * @param method
     * @return dash-based representation of a {@link String}. e.g. getMyProperty => get-my-property
     */
    public static String camelCaseToDashBased(final String string) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            final char character = string.charAt(i);
            if (i != 0 && Character.isUpperCase(character)) {//If upper case and not first character append '-'
                builder.append("-").append(Character.toLowerCase(character));
            } else if (Character.isUpperCase(character)) {
                builder.append(Character.toLowerCase(character));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    public static String constantToCamelCase(String name) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}