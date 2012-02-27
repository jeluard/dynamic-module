
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.mule.tools.module.discovery.MavenRepositoryDiscoverer;
import org.mule.tools.module.loader.JarLoader;
import org.mule.tools.module.model.Module;
import org.mule.tools.module.model.Processor;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author julien
 */
public class Main {
   public static void main(String[] args) throws Exception {
       final List<URL> urls = new MavenRepositoryDiscoverer(MavenRepositoryDiscoverer.DEFAULT_LOCAL_REPOSITORY, MavenRepositoryDiscoverer.defaultMuleForgeRepositories()).listDependencies("mule-module-sfdc", "4.1.1");
       final org.mule.tools.module.model.Package pack = new JarLoader().load(urls);
       System.out.println(pack.getMetadata().getIcons());
       for (final Processor processor : pack.getModule().getProcessors()) {
           System.out.println(processor);
       }
   } 
}
