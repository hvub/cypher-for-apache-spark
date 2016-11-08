package org.opencypher.spark.impl.frame

import org.apache.spark.sql.Dataset
import org.opencypher.spark.api.types.CTNode
import org.opencypher.spark.api.value.CypherNode
import org.opencypher.spark.impl._

object OptionalAllNodes extends FrameCompanion {

  def apply(fieldSym: Symbol)(implicit context: PlanningContext): StdCypherFrame[CypherNode] = {
    val (_, sig) = StdFrameSignature.empty.addField(fieldSym -> CTNode.nullable)

    CypherNodes(context.nodes)(sig)
  }

  private final case class CypherNodes(input: Dataset[CypherNode])(sig: StdFrameSignature) extends NodeFrame(sig) {

    override def execute(implicit context: RuntimeContext): Dataset[CypherNode] = {
      if (input.rdd.isEmpty())
        context.session.createDataset[CypherNode](Seq(null))(context.cypherNodeEncoder)
      else input
    }
  }
}