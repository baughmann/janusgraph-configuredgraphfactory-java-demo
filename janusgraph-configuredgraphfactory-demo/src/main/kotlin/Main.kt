import com.benasher44.uuid.Uuid
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.janusgraph.graphdb.management.ConfigurationManagementGraph
import org.janusgraph.graphdb.management.JanusGraphManager
import org.apache.tinkerpop.gremlin.server.Settings
import org.janusgraph.core.ConfiguredGraphFactory
import org.janusgraph.core.JanusGraph
import org.janusgraph.diskstorage.configuration.BasicConfiguration
import org.janusgraph.diskstorage.configuration.ModifiableConfiguration
import org.janusgraph.diskstorage.configuration.backend.CommonsConfiguration
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration
import org.janusgraph.graphdb.database.StandardJanusGraph
import org.janusgraph.util.system.ConfigurationUtil

import java.io.File

fun main(args: Array<String>) {
    JanusGraphConfiguredGraphFactoryDemo().run()
}

class JanusGraphConfiguredGraphFactoryDemo {
//    private val confFolder = File("../docker/conf")
    private val configurationGraphConfigFile = File("../docker/conf/janusgraph-cql-configurationgraph.properties")
    private val templateConfigFile = File("../docker/conf/janusgraph-cql-es-server.properties")

    /**
     * Sets up the ConfigureGraphManager
     */
    private fun configureGraphManager(): ConfigurationManagementGraph {
        // Much inspiration taken from https://gist.github.com/mchandrasekar/5aa505d8dafc2ea836c0f6b71a31cac4
        // stupid, but necessary to create the singleton
        JanusGraphManager(Settings())

        val instanceId = Uuid.randomUUID().toString()
        val base = CommonsConfiguration(ConfigurationUtil.loadPropertiesConfig(configurationGraphConfigFile))
        base.set("graph.graphname", "mgmt")
        base.set("graph.unique-instance-id", instanceId)
        // i really don't have an idea why this is necessary, or the other options besides 'tmp'. I think 'tmp' was taken from janusgraph tests
        base.set("storage.lock.local-mediator-group", "tmp")

        val local = ModifiableConfiguration(GraphDatabaseConfiguration.ROOT_NS, base, BasicConfiguration.Restriction.NONE)
        // i don't know the difference between the second and last parameters.
        val config = GraphDatabaseConfiguration(base, local, instanceId, local)

        println("Opened configuration graph with ID $instanceId")
        return ConfigurationManagementGraph(StandardJanusGraph(config))
    }

    /**
     * Sets up the graph template (re-used by default for every new graph, if no config provided). Needs to be called before creating a new graph
     */
    private fun configureTemplates() {
        if(ConfiguredGraphFactory.getTemplateConfiguration() == null) {
            val conf = ConfigurationUtil.loadPropertiesConfig(templateConfigFile)
            conf.clearProperty("graph.graphname")
            ConfiguredGraphFactory.createTemplateConfiguration(conf)
        }
    }

    /**
     * Creates a new graph with the given name, or open it if it already exists
     */
    private fun getOrCreateGraph(name: String): JanusGraph {
        return if(ConfiguredGraphFactory.getGraphNames().contains(name)) {
            ConfiguredGraphFactory.open(name)
        } else {
            return ConfiguredGraphFactory.create(name)
        }
    }

    /**
     * Creates a new graph with the given name, or open it if it already exists
     *
     * @param name the name of the graph to create
     * @param createANode whether to create a node in the graph
     */
    fun run(graphName: String = "myNewGraph", createANode: Boolean = true) {
        // don't actually need to use the return value since the singleton `ConfiguredGraphFactory` gets
        //      created when the `ConfigurationManagementGraph` (as long as JanusGraphManager is initialized)
        configureGraphManager()
        configureTemplates()
        val graph = getOrCreateGraph(graphName)
        println("Opened graph '${graphName}'")

        if(createANode) {
            // this may look dumb, but because we have to bypass the gremlin server, transactions are not automatically committed
            val tx = graph.tx()
            val g = tx.begin<GraphTraversalSource>()
            val v = g.addV("testNode").property("isTeapot", "I am a teapot").next()
            // don't forget to commit!
            tx.commit()
            println("Created vertex with ID ${v.id()}")
        }
    }
}