package org.distfp.sts

import org.specs2.mutable.Specification

import StsDelim._

class TextSpec extends Specification {
  "UnsignedText" should {
    "raise SlmException if the text contains the separator" in {
      val lines = Array("one", "two", delimSeparator + "three", "four")
      StsUnsignedText(lines) must throwA[StsException]
    }

    "succeed if the text does not contain any separator" in {
      val lines = Jabberwocky(0)
      StsUnsignedText(lines)
      true
    }
  }
}
