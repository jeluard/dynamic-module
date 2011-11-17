package org.mule.tools.module.helper;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helper methods for jar files.
 */
public final class Jars {

    private Jars() {
    }

    /**
     * @param url
     * @return all {@link File} names contained in specified jar
     * @throws IOException 
     */
    public static List<String> allFileNames(final URL url) throws IOException {
        final ZipInputStream jarStream = new ZipInputStream(url.openStream());
        ZipEntry entry = null;
        final List<String> allNames = new LinkedList<String>();
        while((entry = jarStream.getNextEntry()) != null) {
            allNames.add(entry.getName());
        }
        return allNames;
    }

}