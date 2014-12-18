package starschema.slm4j

object Delim {
    val delimMarker = "-----"

    private def delim(s: String) = s"$delimMarker $s $delimMarker"

    val LICENSE_BEGIN   = delim("BEGIN LICENSE")
    val LICENSE_END     = delim("END LICENSE")
    val SIGNATURE_BEGIN = delim("BEGIN SIGNATURE")
    val SIGNATURE_END   = delim("END SIGNATURE")
}
