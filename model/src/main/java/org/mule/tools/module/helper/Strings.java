package org.mule.tools.module.helper;

import com.google.common.base.Preconditions;

public final class Strings {

    private Strings() {
    }

    public static String capitalize(final String string) {
        Preconditions.checkArgument(!com.google.common.base.Strings.isNullOrEmpty(string));

        return new StringBuilder(string.length()).append(Character.toTitleCase(string.charAt(0))).append(string.substring(1)).toString();
    }

}