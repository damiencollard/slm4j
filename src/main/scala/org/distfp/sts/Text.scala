package org.distfp.sts

trait Text {
  def lines: Seq[String]
}

case class UnsignedText(lines: Seq[String]) extends Text {
  if (lines exists (_.startsWith(Delim.delimSeparator)))
    throw new StsException("Unsigned text contains a line with a license marker")
}

case class SignedText(lines: Seq[String], signature: Array[Byte]) extends Text
