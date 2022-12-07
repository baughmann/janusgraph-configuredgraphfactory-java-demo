# JanusGraph ConfiguredGraphFactory Kotlin (Java) Example
Infinite thanks to [@FlorianHockmann](https://github.com/FlorianHockmann/) on the JanusGraph Discord server for his help! I knew I had to be doing something wrong in my first iteration.

## Description
I had the goal of programmatically working with `ConfiguredGraphFactory` to create separate graphs for the purposes of multitenancy. Since I was new to JanusGraph and the [docs for `ConfiguredGraphFactory`](https://docs.janusgraph.org/operations/configured-graph-factory/#configuring-janusgraph-server-for-configuredgraphfactory) didn't give a Java example, I scoured the internet and the JanusGraph source for options.

With help from [@FlorianHockmann](https://github.com/FlorianHockmann/) on the JanusGraph discord, I was able to modify this example to use **only the Gremlin Driver**, helping to keep the application more Gremlin-implementation-agnostic (though still not entirely).

As per usual, I was overthinking it. The basic workflow is:
1. Setup the configuration files
    - `janusgraph-cql-configurationgraph.properties`: The configuration file for the management graph (graph of graphs)
    - `janusgraph-template.properties`: The base template for new user graphs
    - `janusgraph-server.yaml`: The Gremlin server config specifying the ConfigurationManagementGraph to be created by default and the Groovy scripts to run to create the user graph template
2. Setup the Groovy scripts
    - `configure-graph-template.groovy`: A Groovy script to create the user graph template if it has not already been loaded
    - `empty-sample.groovy`: The default Groovy script included with JanusGraph. Some pieces need to be commented out to avoid throwing errors, or the reference to it needs to be removed from the `janusgraph-server.yaml`
3. In Java/Kotlin/Scala:
    - Connect to the server using Gremlin [`Cluster` API](https://tinkerpop.apache.org/javadocs/current/core/org/apache/tinkerpop/gremlin/driver/Cluster.html)
    - Submit a Groovy script to JanusGraph's Gremlin server to create the graph using the Gremlin [`Client` API](https://tinkerpop.apache.org/javadocs/current/core/org/apache/tinkerpop/gremlin/driver/Client.html)
    - Open the graph using [`withRemote()`](https://tinkerpop.apache.org/javadocs/current/full/org/apache/tinkerpop/gremlin/process/traversal/AnonymousTraversalSource.html#withRemote-org.apache.tinkerpop.gremlin.process.remote.RemoteConnection-)
    - Use the traversal as you would normally

## Files of Importance
|File                   |Description                        |
|-------------|-------------------|
|`docker/conf/janusgraph-cql-configurationgraph.properties`|The configuration file for the Graph Management Graph--the graph that contains metadata about all of the graphs inside the JanusGraph database |
|`docker/conf/janusgraph-template.properties`|The configuration file used as a template for creating new graphs (besides the management graph) |
|`docker/conf/janusgraph-server.yaml`|The configuration file for the gremlin server (I think). This file includes the changes outlined in the [JanusGraph docs](https://docs.janusgraph.org/operations/configured-graph-factory/#configuring-janusgraph-server-for-configuredgraphfactory)|
|`scrips/empty-sample.groovy`|Derfault Groovy script used by janusgraph to set the global `g` vaiable. Obviously, we cannot use this since there is no default graph. It throws an error if you leave it in.|
|`scripts/configure-graph-template.groovy`|A custom file I created to add the graph template configuration from the ``docker/conf/janusgraph-template.properties` to the ConfiguredManagementGraph if it does not already exists. Without this template, you will not be able to create new graphs.|
|`janusgraph-configuredgraphfactory-demo/src/main/kotlin/Main.kt`|A Kotlin file showing an example of how to programmatically|
|`janusgraph-configuredgraphfactory-demo/build.gradle`|The dependencies list. Right now, all that is needed is `org.apache.tinkerpop:gremlin-driver:3.5.4` (`3.5.4` is needed until JanusGraph is compatible with newer versions of Tinkerpop 3)|


## Running
### Networking
First, you need to add the following entries to your hosts file. If you don't you won't be able to run the Kotlin application unless you do so from a docker container inside the `jce-network` network that is specified in the `docker-compose.yaml` file:
```
127.0.0.1   jce-janusgraph  # the main janusgraph server
127.0.0.1   jce-cassandra   # the cassandra instance
```
### Containers
Run `docker compose up` from the `docker` directory and wait until you see that the Gremlin server has started successfully.

### Application
Next, just run `Main.kt` inside the Kotlin application. Feel free to change the `graphName` and `createANode` parameters in the `JanusGraphConfiguredGraphFactoryDemo.run()` method.

### What to expect
You should see the application logs state that a new graph was created and a new node was added. To verify this from the Gremlin console, run the following commands:
1. `gremlin.sh`
2. `:remote connect tinkerpop.server conf/remote.yaml session` -- note the `session` on the end here. It allows us to save session variables
3. `:remote console` to run all commands against the remote server
4. `ConfiguredGraphFactory.getGraphNames()` should display a list of available graphs
5. `graph = ConfiguredGraphFactory.open("myNewGraph")` to get an instance of the graph that was created
6. `g = graph.traversal()` to get a traversal to operate against the graph
7. `g.V()` should display the same ID that was output by the Kotlin application
8. Celebrate!