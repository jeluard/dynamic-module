Dynamic Module allows to programmatically discover/manipulate [Mule modules](http://www.mulesoft.org/muleforge/cloud-connectors) generated by [Mule devkit](http://www.mulesoft.org/documentation/display/DEVKIT/Home).

# Discovery

Most of modules are available on MuleForge [Nexus repository](https://repository.mulesoft.org/nexus/index.html#welcome).
Available modules/versions can be listed programmatically.

```java
final NexusBrowser browser = new NexusBrowser();

//List all MuleSoft modules
final List<List<NexusArtifact>> groups = browser.listArtifacts();

//Or only their artifactId
final List<String> artifactIds = browser.listArtifactIds();

//Or all versions of a specific module
final List<String> versions = browser.listArtifactVersions("artifactId");
```

A module Jar plus all its dependencies can retrieved/locally installed using maven coordinates. Under the hood [MuleForge](http://www.mulesoft.org/muleforge) will be accessed, dependencies resolved, artifacts downloaded and locally installed.

```java
//Using default local repository
final List<URL> urls = new MavenRepositoryDiscoverer().listDependencies("artifactId", "version");

//Or a specific local repository
final File localRepository = ...;
final List<URL> urls = new MavenRepositoryDiscoverer(localRepository).listDependencies("artifactId", "version");

//Or a MuleForge remote repositories
final MavenRepositoryDiscoverer discoverer = new MavenRepositoryDiscoverer(localRepository, MavenRepositoryDiscoverer.defaultMuleForgeRepositories());
final List<URL> urls = discoverer.listDependencies("artifactId", "version");
```

# Meta model generation

A Module model can be generated from your module Jars. All Module meta data are then accessible.

```java
//From some URLs
final Module module = new JarLoader().load(urls);
//Or from well know module class
final Module module = new Loader().load(module, connectionManager);

//Browse meta data
final List<Module.Parameter> parameters = module.getParameters();
final List<Module.Processor> processors = module.getProcessors();
final List<Module.Source> sources = module.getSources();
final List<Module.Transformer> transformers = module.getTransformers();
```

# Dynamic manipulation

Once you have this model you can use a DynamicModule to dynamically invoke [Processor](http://www.mulesoft.org/documentation/display/DEVKIT/Creating+Message+Processors) and subscribe to [Source](http://www.mulesoft.org/documentation/display/DEVKIT/Creating+Message+Sources).

```java
final DynamicModule dynamicModule = new DynamicModule(module, parameterValues);
dynamicModule.invoke("name", methodParameters);
dynamicModule.subscribe("source", sourceParameters, new Listener<T>() {
  public void onEvent(T event) {
    System.out.printl("Received: "+event);
  }
});
dynamicModule.unsubscribe("source");
dynamicModule.close();
```

A specialised DynamicModule, RetryingDynamicModule, provides retry capacities to invocations. You can provide a custom [RetryPolicyTemplate](http://www.mulesoft.org/docs/site/3.2.0/apidocs/org/mule/api/retry/RetryPolicyTemplate.html) that will be used to retry invocation in case of failures.
Mule ships with a number of default [retry policies](http://www.mulesoft.org/common-retry-policies).

```java
final RetryPolicyTemplate retryPolicyTemplate = ...;
final RetryingDynamicModule dynamicModule = new RetryingDynamicModule(module, parameterValues, retryPolicyTemplate);
dynamicModule.invoke("name", methodParameters);
```

# Example

```java
final File localRepository = ...;
final MavenRepositoryDiscoverer discoverer = new MavenRepositoryDiscoverer(localRepository, MavenRepositoryDiscoverer.defaultMuleForgeRepositories());
final List<URL> urls = discoverer.listDependencies("mule-module-sfdc", "4.0-SNAPSHOT");

final Module module = new JarLoader().load(urls);
module.setUsername("username");
module.setPassword("password");
module.setSecurityToken("securityToken");

final Map<String, Object> moduleParameters = new HashMap<String, Object>();
moduleParameters.put("url", new URL("https://test.salesforce.com/services/Soap/u/23.0"));

final DynamicModule dynamicModule = new DynamicModule(module, moduleParameters);

final Map<String, Object> methodParameters = new HashMap<String, Object>();
methodParameters.put("type", "Account");
final List<Map<String, Object>> objects = new LinkedList<Map<String, Object>>();
final Map<String, Object> object = new HashMap<String, Object>();
object.put("Name", "Account name");
objects.add(object);
methodParameters.put("objects", objects);

dynamicModule.invoke("create", methodParameters);
dynamicModule.dispose();
```