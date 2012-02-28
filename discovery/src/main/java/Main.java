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
