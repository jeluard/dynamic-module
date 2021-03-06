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

package org.mule.tools.module.discovery;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.mule.tools.module.discovery.wagon.ManualWagonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

/**
 * Helper methods for listing dependencies of MuleForge artifact.
 */
public final class MavenRepositoryDiscoverer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenRepositoryDiscoverer.class.getPackage().getName());

    private final File localRepository;
    public static final File DEFAULT_LOCAL_REPOSITORY = new File(System.getProperty("user.home")+"/.m2/repository");
    private final List<RemoteRepository> repositories;
    private static final String DEFAULT_GROUP_ID = "org.mule.modules";
    private static final String DEFAULT_EXTENSION = "jar";
    private static final String DEFAULT_SCOPE = "compile";

    public MavenRepositoryDiscoverer() {
        this(MavenRepositoryDiscoverer.DEFAULT_LOCAL_REPOSITORY);
    }

    public MavenRepositoryDiscoverer(final File localRepository) {
        this(localRepository, Collections.<RemoteRepository>emptyList());
    }

    public MavenRepositoryDiscoverer(final File localRepository, final List<RemoteRepository> repositories) {
        this.localRepository = localRepository;
        this.repositories = repositories;
    }

    public static List<RemoteRepository> defaultMuleForgeRepositories() {
        final List<RemoteRepository> repositories = new LinkedList<RemoteRepository>();
        repositories.add(new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/"));
        repositories.add(new RemoteRepository("muleforge", "default", "http://repository.mulesoft.org/releases/"));
        repositories.add(new RemoteRepository("muleforge2", "default", "https://repository.mulesoft.org/nexus/content/repositories/releases/"));
        repositories.add(new RemoteRepository("muleforge-snapshots", "default", "http://repository.mulesoft.org/snapshots/"));
        repositories.add(new RemoteRepository("jboss", "default", "http://repository.jboss.org/nexus/content/repositories/"));
        return repositories;
    }

    public final List<URL> listDependencies(final String artifactId, final String version) throws DependencyCollectionException, DependencyResolutionException, MalformedURLException {
        return listDependencies(MavenRepositoryDiscoverer.DEFAULT_GROUP_ID, artifactId, version);
    }

    public final List<URL> listDependencies(final String groupId, final String artifactId, final String version) throws DependencyCollectionException, DependencyResolutionException, MalformedURLException {
        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(WagonProvider.class, new ManualWagonProvider());
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
            
        final RepositorySystem system = locator.getService(RepositorySystem.class);

        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        final LocalRepository localRepo = new LocalRepository(this.localRepository);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

        final Dependency dependency = new Dependency(new DefaultArtifact(groupId, artifactId, MavenRepositoryDiscoverer.DEFAULT_EXTENSION, version), MavenRepositoryDiscoverer.DEFAULT_SCOPE, false);
        final CollectRequest collectRequest = new CollectRequest(dependency, this.repositories);
        final DependencyNode node = system.collectDependencies(session, collectRequest).getRoot();

        system.resolveDependencies(session, new DependencyRequest(node, null));

        final PreorderNodeListGenerator nodeListGenerator = new PreorderNodeListGenerator();
        node.accept(nodeListGenerator);

        final List<URL> urls = new LinkedList<URL>();
        for (final Artifact artifact : nodeListGenerator.getArtifacts(true)) {
            if (artifact.getFile() == null) {
                if (MavenRepositoryDiscoverer.LOGGER.isDebugEnabled()) {
                    MavenRepositoryDiscoverer.LOGGER.debug("Failed to resolve artifact <"+artifact.getArtifactId()+">; it will be ignored");
                }

                continue;
            }

            urls.add(artifact.getFile().toURI().toURL());
        }
        return urls;
    }

}