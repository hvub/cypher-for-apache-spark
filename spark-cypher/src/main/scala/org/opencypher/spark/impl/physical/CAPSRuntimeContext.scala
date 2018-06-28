/*
 * Copyright (c) 2016-2018 "Neo4j Sweden, AB" [https://neo4j.com]
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
 *
 * Attribution Notice under the terms of the Apache License 2.0
 *
 * This work was created by the collective efforts of the openCypher community.
 * Without limiting the terms of Section 6, any Derivative Work that is not
 * approved by the public consensus process of the openCypher Implementers Group
 * should not be described as “Cypher” (and Cypher® is a registered trademark of
 * Neo4j Inc.) or as "openCypher". Extensions by implementers or prototypes or
 * proposals for change that have been documented or implemented should only be
 * described as "implementation extensions to Cypher" or as "proposed changes to
 * Cypher that are not yet approved by the openCypher community".
 */
package org.opencypher.spark.impl.physical

import org.opencypher.okapi.api.graph.QualifiedGraphName
import org.opencypher.okapi.api.value.CypherValue.CypherMap
import org.opencypher.okapi.relational.api.physical.RuntimeContext
import org.opencypher.spark.api.CAPSSession
import org.opencypher.spark.impl.physical.operators.CAPSPhysicalOperator
import org.opencypher.spark.impl.table.SparkFlatRelationalTable._
import org.opencypher.spark.impl.{CAPSGraph, CAPSRecords}

import scala.collection.mutable

object CAPSRuntimeContext {
  def empty(implicit session: CAPSSession) = CAPSRuntimeContext(CypherMap.empty, _ => None, mutable.Map.empty, mutable.Map.empty)
}

case class CAPSRuntimeContext(
  parameters: CypherMap,
  resolve: QualifiedGraphName => Option[CAPSGraph],
  cache: mutable.Map[CAPSPhysicalOperator, DataFrameTable],
  patternGraphTags: mutable.Map[QualifiedGraphName, Set[Int]])(implicit val session: CAPSSession)
  extends RuntimeContext[DataFrameTable, CAPSRecords, CAPSGraph]
