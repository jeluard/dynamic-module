/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mule.tools.module.helper;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Very dumb converter.
 */
public final class Converters {

    public static Object convert(final Class<?> type, final String string) {
        if (type.equals(int.class)) {
            return Integer.parseInt(string);
        } else if (type.equals(URL.class)) {
            try {
                return new URL(string);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Got exception while converting <"+string+"> to <"+type+">", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported type <"+type+">");
        }
    }

}