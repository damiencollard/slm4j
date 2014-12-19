package org.distfp.sts

trait Text {
  def lines: Array[String]
}

case class UnsignedText(lines: Array[String]) extends Text {
  if (lines exists (_.startsWith(Delim.delimSeparator)))
    throw new StsException("Unsigned text contains a line with a license marker")
}

case class SignedText(lines: Array[String], signature: Array[Byte]) extends Text
