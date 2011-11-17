package org.mule.tools.module.discovery;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;

import org.mule.tools.module.ManualWagonProvider;

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
public final class MuleForgeDiscovery {

    private static final String DEFAULT_GROUP_ID = "org.mule.modules";
    private static final String DEFAULT_EXTENSION = "jar";
    private static final String DEFAULT_SCOPE = "compile";
    private final List<RemoteRepository> repositories;

    public MuleForgeDiscovery() {
        this(MuleForgeDiscovery.defaultRepositories());
    }

    public MuleForgeDiscovery(final List<RemoteRepository> repositories) {
        this.repositories = repositories;
    }

    public static List<RemoteRepository> defaultRepositories() {
        final List<RemoteRepository> repositories = new LinkedList<RemoteRepository>();
        repositories.add(new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/"));
        repositories.add(new RemoteRepository("muleforge", "default", "http://repository.mulesoft.org/releases/"));
        repositories.add(new RemoteRepository("muleforge-snapshots", "default", "http://repository.mulesoft.org/snapshots/"));
        repositories.add(new RemoteRepository("jboss", "default", "http://repository.jboss.org/nexus/content/repositories/"));
        return repositories;
    }

    public final List<URL> listDependencies(final String artifactId, final String version) throws DependencyCollectionException, DependencyResolutionException, MalformedURLException {
        return listDependencies(MuleForgeDiscovery.DEFAULT_GROUP_ID, artifactId, version);
    }

    public final List<URL> listDependencies(final String groupId, final String artifactId, final String version) throws DependencyCollectionException, DependencyResolutionException, MalformedURLException {
        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(WagonProvider.class, new ManualWagonProvider());
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
            
        final RepositorySystem system = locator.getService(RepositorySystem.class);

        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        final LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

        final Dependency dependency = new Dependency(new DefaultArtifact(groupId, artifactId, MuleForgeDiscovery.DEFAULT_EXTENSION, version), MuleForgeDiscovery.DEFAULT_SCOPE);
        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        for (final RemoteRepository remoteRepository : this.repositories) {
            collectRequest.addRepository(remoteRepository);
        }

        final DependencyNode node = system.collectDependencies(session, collectRequest).getRoot();

        final DependencyRequest dependencyRequest = new DependencyRequest(node, null);

        system.resolveDependencies(session, dependencyRequest);

        final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept( nlg );

        final List<URL> urls = new LinkedList<URL>();
        for (final Artifact artifact : nlg.getArtifacts(true)) {
            if (artifact.getFile() == null) {
                //TODO Log
                continue;
            }

            urls.add(artifact.getFile().toURI().toURL());
        }
        return urls;
    }

}