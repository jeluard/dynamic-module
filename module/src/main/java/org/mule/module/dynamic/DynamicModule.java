/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.module.dynamic;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.tools.module.browsing.NexusBrowser;
import org.mule.tools.module.discovery.MavenRepositoryDiscoverer;
import org.mule.tools.module.loader.JarLoader;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.nexus.client.NexusClientException;
import org.sonatype.nexus.client.NexusConnectionException;

/**
 * Dynamic Module
 *
 * @author MuleSoft, Inc.
 */
@Module(name="dynamic", schemaVersion="1.0-SNAPSHOT")
public class DynamicModule {

    private final NexusBrowser browser = new NexusBrowser();

    /**
     * List all MuleForge modules.
     *
     * {@sample.xml ../../../doc/Dynamic-module.xml.sample dynamic:list-ids}
     *
     * @return ee
     */
    @Processor
    public Set<String> listIds() throws NexusClientException, NexusConnectionException {
        return new TreeSet<String>(this.browser.listArtifactIds());
    }

    /**
     * List all MuleForge modules.
     *
     * {@sample.xml ../../../doc/Dynamic-module.xml.sample dynamic:list-versions}
     *
     * @param id Module id
     * @param includeSnapshots true to include snapshot versions
     * @return ee
     */
    @Processor
    public Set<String> listVersions(final String id, final boolean includeSnapshots) throws NexusClientException, NexusConnectionException {
        final Set<String> allVersions = new TreeSet<String>(this.browser.listArtifactVersions(id));
        if (!includeSnapshots) {
            for (final Iterator<String> it = allVersions.iterator(); it.hasNext();) {
                final String version = it.next();
                if (version.endsWith("SNAPSHOT")) {
                    it.remove();
                }
            }
        }
        return allVersions;
    }

    /**
     * List all MuleForge modules.
     *
     * {@sample.xml ../../../doc/Dynamic-module.xml.sample dynamic:list-versions}
     *
     * @param id Module id
     * @param version Module version
     * @return ee
     */
    @Processor
    public org.mule.tools.module.model.Module module(final String id, final String version) throws NexusClientException, NexusConnectionException, DependencyCollectionException, DependencyResolutionException, MalformedURLException, IOException {
        try {
            final MavenRepositoryDiscoverer discoverer = new MavenRepositoryDiscoverer(new File("."), MavenRepositoryDiscoverer.defaultMuleForgeRepositories());
            final List<URL> urls = discoverer.listDependencies(id, version);
            return new JarLoader().load(urls).getModule();
        } catch (Exception e) {
            return new org.mule.tools.module.model.Module("", "", e.getMessage(), Collections.<org.mule.tools.module.model.Parameter>emptyList(), Collections.<org.mule.tools.module.model.Processor>emptyList(), Collections.<org.mule.tools.module.model.Source>emptyList(), Collections.<org.mule.tools.module.model.Transformer>emptyList(), null);
        }
    }

}