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

package org.mule.module.dynamic;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.tools.module.browsing.NexusBrowser;
import org.mule.tools.module.discovery.MavenRepositoryDiscoverer;
import org.mule.tools.module.loader.JarLoader;
import org.mule.tools.module.model.Parameter;
import org.mule.tools.module.model.Source;
import org.mule.tools.module.model.Transformer;
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
        //TODO move to list w/o doublons
        if (true) {
            Set<String> versions = new HashSet<String>();
            versions.add("module1");
            versions.add("module2");
            versions.add("module3");
            return versions;
        }
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
        if (true) {
            Set<String> versions = new HashSet<String>();
            versions.add("1.0");
            versions.add("2.0");
            versions.add("3.0");
            if (includeSnapshots) versions.add("3.0-SNAPSHOT");
            return versions;
        }

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
        if (true) {
            final Parameter parameter1 = new Parameter("parameter1", int.class, true, "default1");
            final Parameter parameter2 = new Parameter("parameter2", int.class, true, "default2");
            final Parameter parameter3 = new Parameter("parameter3", List.class, true, "default3");
            final Parameter parameter4 = new Parameter("parameter4", float.class, true, "default4");
            final Parameter parameter5 = new Parameter("parameter5", int.class, true, "default5");
            final List<Parameter> parameters = new LinkedList<Parameter>();
            parameters.add(parameter1);
            parameters.add(parameter2);
            parameters.add(parameter3);
            parameters.add(parameter4);
            parameters.add(parameter5);
            final org.mule.tools.module.model.Processor processor1 = new org.mule.tools.module.model.Processor("processor1", "friendlyName", "type", parameters, "type", true);
            final org.mule.tools.module.model.Processor processor2 = new org.mule.tools.module.model.Processor("processor2", "friendlyName", "type", parameters, "type", true);
            final org.mule.tools.module.model.Processor processor3 = new org.mule.tools.module.model.Processor("processor3", "friendlyName", "type", parameters, "type", true);
            final org.mule.tools.module.model.Processor processor4 = new org.mule.tools.module.model.Processor("processor4", "friendlyName", "type", parameters, "type", true);
            final List<org.mule.tools.module.model.Processor> processors = new LinkedList<org.mule.tools.module.model.Processor>();
            processors.add(processor1);
            processors.add(processor2);
            processors.add(processor3);
            processors.add(processor4);
            final Source source1 = new Source("source1", "friendlyName", "type", parameters);
            final Source source2 = new Source("source2", "friendlyName", "type", parameters);
            final Source source3 = new Source("source3", "friendlyName", "type", parameters);
            final Source source4 = new Source("source4", "friendlyName", "type", parameters);
            final Source source5 = new Source("source5", "friendlyName", "type", parameters);
            final List<Source> sources = new LinkedList<Source>();
            sources.add(source1);
            sources.add(source2);
            sources.add(source3);
            sources.add(source4);
            sources.add(source5);
            final Transformer transformer1 = new Transformer("type", 1, new Class[]{int.class});
            final Transformer transformer2 = new Transformer("type", 1, new Class[]{int.class});
            final Transformer transformer3 = new Transformer("type", 1, new Class[]{int.class});
            final Transformer transformer4 = new Transformer("type", 1, new Class[]{int.class});
            final Transformer transformer5 = new Transformer("type", 1, new Class[]{int.class});
            final List<Transformer> transformers = new LinkedList<Transformer>();
            transformers.add(transformer1);
            transformers.add(transformer2);
            transformers.add(transformer3);
            transformers.add(transformer4);
            transformers.add(transformer5);
            return new org.mule.tools.module.model.Module("module1", "3.2.0", "connector", parameters, processors, sources, transformers, null);
        }
        try {
            final MavenRepositoryDiscoverer discoverer = new MavenRepositoryDiscoverer(new File("."), MavenRepositoryDiscoverer.defaultMuleForgeRepositories());
            final List<URL> urls = discoverer.listDependencies(id, version);
            return new JarLoader().load(urls).getModule();
        } catch (Exception e) {
            return new org.mule.tools.module.model.Module("", "", e.getMessage(), Collections.<org.mule.tools.module.model.Parameter>emptyList(), Collections.<org.mule.tools.module.model.Processor>emptyList(), Collections.<org.mule.tools.module.model.Source>emptyList(), Collections.<org.mule.tools.module.model.Transformer>emptyList(), null);
        }
    }

}