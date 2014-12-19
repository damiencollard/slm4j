package org.distfp.sts

import org.specs2.mutable.Specification

import Delim._

class TextSpec extends Specification {
  "UnsignedText" should {
    "raise SlmException if the text contains the separator" in {
      val lines = Array("one", "two", delimSeparator + "three", "four")
      UnsignedText(lines) must throwA[SlmException]
    }

    "succeed if the text does not contain any separator" in {
      val lines = Array(
        "Beware the Jabberwock, my son!",
        "The jaws that bite, the claws that catch!",
        "Beware the Jubjub bird, and shun",
        "The frumious Bandersnatch!"
      )
      UnsignedText(lines)
      true
    }
  }
}
