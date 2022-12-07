import org.apache.tinkerpop.gremlin.driver.Client
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource


fun main(args: Array<String>) {
    val graphName = "myGraph"
    val createANode = true

    val g = openOrCreateGraph(graphName)
    if (createANode) g.addV("person").property("name", "John").next()

    println("Graph '$graphName' has ${g.V().count().next()} vertices and ${g.E().count().next()} edges")
    println("Closing graph '$graphName'...")
    g.close()
}

private fun openOrCreateGraph(graphName: String): GraphTraversalSource {
    val cluster = Cluster.build("localhost").create()
    val client = cluster.connect<Client.ClusteredClient>()
    val existingGraphs = client.submit("ConfiguredGraphFactory.getGraphNames()").all().get()
    val exists = existingGraphs.any { it.string == graphName }
    if (!exists) {
        println("Creating graph '$graphName'...")
        client.submit("ConfiguredGraphFactory.create('$graphName')").all().get()
    } else {
        println("Graph '$graphName' already exists, opening...")
    }
    return traversal().withRemote(DriverRemoteConnection.using(cluster, "${graphName}_traversal"))
}

