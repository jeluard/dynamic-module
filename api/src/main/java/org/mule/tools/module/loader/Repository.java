package org.mule.tools.module.loader;

import java.net.URL;

public interface Repository {

    URL getHomePage();

    /**
     * 
     * @param path
     * @return an {@link URL} pointing to hosted icons; null if none can be found
     */
    URL getIconLocation(String path);

}