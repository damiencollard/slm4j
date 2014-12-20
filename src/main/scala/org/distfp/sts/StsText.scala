package org.distfp.sts

trait StsText {
  def lines: Seq[String]
}

case class StsUnsignedText(lines: Seq[String]) extends StsText {
  if (lines exists (_.startsWith(StsDelim.delimSeparator)))
    throw new StsException("Unsigned text contains a line with a license marker")
}

case class StsSignedText(lines: Seq[String], signature: Array[Byte]) extends StsText
