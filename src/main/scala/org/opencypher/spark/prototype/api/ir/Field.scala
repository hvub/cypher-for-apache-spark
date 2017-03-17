package org.opencypher.spark.prototype.api.ir

final case class Field(name: String) extends AnyVal {
  def escapedName: String = name.replaceAll("`", "``")
}
