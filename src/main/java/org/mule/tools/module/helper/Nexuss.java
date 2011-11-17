package org.mule.tools.module.helper;

import java.lang.reflect.Field;
import java.util.List;
import org.sonatype.nexus.client.NexusClient;
import org.sonatype.nexus.client.rest.NexusRestClient;
import org.sonatype.nexus.rest.model.NexusArtifact;

/**
 * Helper methods for listing artifacts on a Nexus repo.
 */
public final class Nexuss {

    private Nexuss() {
    }

    public static void listConnectors() throws Exception {
        final NexusClient client = new NexusRestClient();
        
        client.connect("https://repository.mulesoft.org/nexus/", null, null);
        final Field field = client.getClass().getDeclaredField("clientHelper");
        field.setAccessible(true);
        final Field field2 = field.get(client).getClass().getDeclaredField("challenge");
        field2.setAccessible(true);
        field2.set(field.get(client), null);
        
        final NexusArtifact template = new NexusArtifact();
        template.setGroupId("org.mule.modules");
        template.setPackaging("mule-module");
        //final Map<NexusArtifact, List<String>> map = new TreeMap<Object, Object>
        final List<NexusArtifact> artifacts = client.searchByGAV(template);
        for (final NexusArtifact artifact : artifacts) {
        }
    }

}