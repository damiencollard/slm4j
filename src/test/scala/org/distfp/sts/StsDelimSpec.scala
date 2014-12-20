package org.distfp.sts

import org.specs2.mutable.Specification

import StsDelim._

class StsDelimSpec extends Specification {
  val testMarkers = Seq("", "dummy", "THING With SpACeS")

  "beginDelim" should {
    "return the provided string prefixed with BEGIN and surrounded by separators" in {
       testMarkers forall { s =>
        beginDelim(s) must_== s"$delimSeparator BEGIN $s $delimSeparator"
      }
    }
  }

  "endDelim" should {
    "return the provided string prefixed with END and surrounded by separators" in {
      testMarkers forall { s =>
        endDelim(s) must_== s"$delimSeparator END $s $delimSeparator"
      }
    }
  }
}
