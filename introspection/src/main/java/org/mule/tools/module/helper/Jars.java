/**
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright 2012 Julien Eluard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     [http://www.apache.org/licenses/LICENSE-2.0]
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.mule.tools.module.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
        try {
            ZipEntry entry = null;
            final List<String> allNames = new LinkedList<String>();
            while((entry = jarStream.getNextEntry()) != null) {
                allNames.add(entry.getName());
            }
            return allNames;
        } finally {
            jarStream.close();
        }
    }

    public static File load(final URL url, final String regex) throws IOException, URISyntaxException {
        if (url == null) {
            throw new IllegalArgumentException("null url");
        }
        if (regex == null) {
            throw new IllegalArgumentException("null regex");
        }

        final ZipInputStream jarStream = new ZipInputStream(url.openStream());
        try {
            ZipEntry entry = null;
            while((entry = jarStream.getNextEntry()) != null) {
                final String name = entry.getName();
                if (name.matches(regex)) {
                    final ZipFile zip = new ZipFile(new File(url.toURI()));
                    final InputStream inputStream = zip.getInputStream(entry);
                    final File file = File.createTempFile("pom", ".temp");
                    final FileOutputStream outputStream = new FileOutputStream(file);
                    final byte[] buffer = new byte[1024];
                    int n;
                    while ((n = inputStream.read(buffer, 0, 1024)) > -1) {
                        outputStream.write(buffer, 0, n);
                    }
                    outputStream.close();
                    inputStream.close();
                    return file;
                }
            }
            return null;
        } finally {
            jarStream.close();
        }
    }

}