# JanusGraph ConfiguredGraphFactory Kotlin (Java) Example
Oh boy, it was quite a process figuring this out. I know someonw will think I'm a saint for this. Whoever you are, I hope the APIs have not changed too much by the time you find this. I would never have figured this out without digging through JanusGraph's source.

## Overview
This is just an example of how to use `ConfigurationManagementGraph` / `ConfiguredGraphFactory` in Java / Kotlin. There are plenty examples doing this from the Gremlin Console on the JanusGraph docs site, but none for an actual Java application.

I'm pretty sure you cannot do this in any non-Java-interoperable lanuages since it requires using JanusGraph's java libraries. You cannot do it with Gremlin alone.

## Main Components
### `docker`
This directory contains a `docker-compose.yaml` file that will spin up a JanusGraph and Gremlin server, a Cassandra instance, and an ElasticSearch instance.

|File                   |Description                        |
|-------------|-------------------|
|`docker/conf/janusgraph-cql-configurationgraph.properties`|The configuration file for the Graph Management Graph--the graph that contains metadata about all of the graphs inside the JanusGraph database |
|`docker/conf/janusgraph-cql-es-server.properties`|The configuration file used as a template for creating new graphs (besides the management graph) |
|`docker/conf/janusgraph-server.yaml`|The configuration file for the gremlin server (I think). This file includes the changes outlined in the [JanusGraph docs](https://docs.janusgraph.org/operations/configured-graph-factory/#configuring-janusgraph-server-for-configuredgraphfactory)|
|`scrips/*`|These files are used by janusgraph to set the global `g` vaiable. Obviously, we cannot use this since there is no default graph. It throws an error if you leave it in.|

### `janusgraph-configuredgraphfactory-demo`
This is the Gradle Kotlin project. The only two files of interest are `janusgraph-configuredgraphfactory-demo/src/main/kotlin/Main.kt` (the actual application code) and `janusgraph-configuredgraphfactory-demo/build.gradle` (contains the necessary dependencies)

## Running
### Networking
First, you need to add the following entries to your hosts file. If you don't you won't be able to run the Kotlin application unless you do so from a docker container inside the `jce-network` network that is specified in the `docker-compose.yaml` file:
```
127.0.0.1   jce-janusgraph  # the main janusgraph server
127.0.0.1   jce-cassandra   # the cassandra instance
127.0.0.1   jce-elastic     # the elasticsearch instance
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