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
final List<String> versions = browser.listArtifactVersions("mule-module-sfdc");
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

# Invocation

A MessageProcessor can be simply invoked using an Invoker.

```java
final MessageProcessor messageProcessor = ...;
final Object muleModule = ...;
final Map<String, Object> parameterValues = ...;
final Map<String, Object> methodParameterValues = ...;
        
final Invoker invoker = new Invoker(messageProcessor, module, parameterValues, 5);
invoker.initialise();
final Object result = invoker.invoke(methodParameterValues);
invoker.close();
```

It might be more convenient to load all Processors dynamically once and executed them using the same `name` you would use in your xml application.
A ModuleInvoker will handle Invoker lifecycle for you.

```java
final Module module = new JarLoader().load(urls);
//Setup this module if needed. For instance Connector will need credentials to be injected.

final ModuleInvoker moduleInvoker = new ModuleInvoker(module, parameterValues);
moduleInvoker.invoke("name", methodParameterValues);
moduleInvoker.close();
```

Module description can be generated from your module Jar.

```java
final Module module = new JarLoader().load(urls);
```

# Example

```java
final File localRepository = ...;
final MavenRepositoryDiscoverer discoverer = new MavenRepositoryDiscoverer(localRepository, MavenRepositoryDiscoverer.defaultMuleForgeRepositories());
final List<URL> urls = discoverer.listDependencies("mule-module-sfdc", "4.0-SNAPSHOT");

final Connector connector = (Connector) new JarLoader().load(urls);
connector.setUsername("username");
connector.setPassword("password");
connector.setSecurityToken("securityToken");

final Map<String, Object> parameterValues = new HashMap<String, Object>();
parameterValues.put("url", new URL("https://test.salesforce.com/services/Soap/u/23.0"));

final ModuleInvoker moduleInvoker = new ModuleInvoker(module, parameterValues);

final Map<String, Object> methodParameterValues = new HashMap<String, Object>();
methodParameterValues.put("type", "Account");
final List<Map<String, Object>> objects = new LinkedList<Map<String, Object>>();
final Map<String, Object> object = new HashMap<String, Object>();
object.put("Name", "Account name");
objects.add(object);
methodParameterValues.put("objects", objects);

moduleInvoker.invoke("create", methodParameterValues);
moduleInvoker.close();
```