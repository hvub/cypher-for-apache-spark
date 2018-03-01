/*
 * Copyright (c) 2016-2018 "Neo4j, Inc." [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.spark.examples

import org.neo4j.driver.v1.{AuthTokens, Session, StatementResult}
import org.opencypher.okapi.api.graph.{GraphName, Namespace, QualifiedGraphName}
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.api.io.file.FileCsvPropertyGraphDataSource
import org.opencypher.spark.api.io.neo4j.Neo4jPropertyGraphDataSource._
import org.opencypher.spark.api.io.neo4j.{Neo4jConfig, Neo4jPropertyGraphDataSource}
import org.opencypher.spark.examples.Neo4jHelpers._

/**
  * Demonstrates connecting a graph from a CSV data source with a graph from a Neo4j data source.
  *
  * Write updates back to the Neo4j database with Cypher queries.
  */
object Neo4jMultipleGraphWorkflow extends App {
  // 1) Create CAPS session
  implicit val session: CAPSSession = CAPSSession.local()

  // 2) Load a graph from a running Neo4j instance. Setup with Neo4j Desktop from https://neo4j.com/download/

  //    Config

  //    Remove the next line and set/store the PW property separately from the application source code.
  System.setProperty("neo4j-pw", "example-pw") // Remove: Do not store passwords in code, set them externally instead.
  val neo4jPw = System.getProperty("neo4j-pw")
  implicit val neo4jConfig = Neo4jConfig(password = Some(neo4jPw))

  //   Load test data into Neo4j
  withBoltSession(loadPersonNetwork)

  val neo4jSource = new Neo4jPropertyGraphDataSource(neo4jConfig)
  val neo4jNamespace = Namespace("neo4j")
  session.registerSource(neo4jNamespace, neo4jSource)
  // Access then graph via its qualified graph name
  val socialNetwork = session.graph(QualifiedGraphName(neo4jNamespace, neo4jDefaultGraphName))

  // 3) Register a File-based data source in the Cypher session
  val csvFolder = getClass.getResource("/csv").getFile
  val csvNamespace = Namespace("csv")
  session.registerSource(csvNamespace, new FileCsvPropertyGraphDataSource(graphFolder = csvFolder))
  // Access the graph via its qualified graph name
  val purchaseNetwork = session.graph(QualifiedGraphName(csvNamespace, GraphName("prod")))

  // 4) Build union of Neo4j social and CSV purchase network
  //    (note that there are no relationships connecting nodes from both graphs)
  val disconnectedGraph = socialNetwork union purchaseNetwork

  // 5) Create new edges between users and customers with the same name
  // TODO: Fix bug that requires "WITH p.name as pName, p"
  val integrationGraph = disconnectedGraph.cypher(
    """|FROM GRAPH AT 'neo4j.graph'
       |MATCH (p:Person)
       |WITH p.name as pName, p
       |FROM GRAPH AT 'csv.prod'
       |MATCH (c:Customer)
       |WHERE pName = c.name
       |RETURN GRAPH OF (p)-[x:IS]->(c)
    """.stripMargin
  ).graph.get

  // 6) Build recommendation graph from disconnected and integration graphs
  val recommendationGraph = disconnectedGraph union integrationGraph

  // 7) Query for product recommendations
  val recommendations = recommendationGraph.cypher(
    """|MATCH (person:Person)-[:FRIEND_OF]-(friend:Person),
       |(friend)-[:IS]->(customer:Customer),
       |(customer)-[:BOUGHT]->(product:Product)
       |RETURN person.name AS for, collect(DISTINCT product.title) AS recommendations""".stripMargin)

  // 8) Use Cypher queries to write the product recommendations back to Neo4j
  withBoltSession { session =>
    recommendations.getRecords.collect.foreach { recommendation =>
      session.run(
        s"""|MATCH (p:Person {name: ${recommendation.get("for").get.toCypherString}})
            |SET p.should_buy = ${recommendation.get("recommendations").get.toCypherString}""".stripMargin)
    }
  }

  // 9. Proof that the write-back to Neo4j worked, retrieve and print updated Neo4j results
  val updatedNeo4jSource = new Neo4jPropertyGraphDataSource(neo4jConfig)
  val updatedNeo4jNamespace = Namespace("updated-neo4j")
  session.registerSource(updatedNeo4jNamespace, updatedNeo4jSource)
  val socialNetworkWithRanks = session.graph(QualifiedGraphName(updatedNeo4jNamespace, neo4jDefaultGraphName))
  socialNetworkWithRanks.cypher("MATCH (p) RETURN p.name, p.should_buy").show

}

object Neo4jHelpers {

  def loadPersonNetwork(session: Session)(implicit neo4jConfig: Neo4jConfig): Unit = {
    val isEmpty: Boolean = {
      val countResult: StatementResult = session.run("MATCH (n) RETURN COUNT(n) as count")
      countResult.hasNext && countResult.next.get("count").asInt == 0
    }
    if (!isEmpty) {
      throw new UnsupportedOperationException(
        s"Neo4j database contains data already, will not write example data into it.")
    } else {
      session.run(
        s"""|CREATE (a:Person { name: 'Alice', age: 10 })
            |CREATE (b:Person { name: 'Bob', age: 20})
            |CREATE (c:Person { name: 'Carol', age: 15})
            |CREATE (a)-[:FRIEND_OF { since: '23/01/1987' }]->(b)
            |CREATE (b)-[:FRIEND_OF { since: '12/12/2009' }]->(c)""".stripMargin)
    }
  }

  def withBoltSession[T](f: Session => T)(implicit neo4jConfig: Neo4jConfig): T = {
    val driver = org.neo4j.driver.v1.GraphDatabase.driver(
      neo4jConfig.uri, AuthTokens.basic(neo4jConfig.user, neo4jConfig.password.get), neo4jConfig.boltConfig())
    val session = driver.session()
    try {
      f(session)
    } finally {
      session.close()
    }
  }

}
