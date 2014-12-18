package starschema.slm4j

object Delim {
    val LICENSE_BEGIN   = delim("BEGIN LICENSE")
    val LICENSE_END     = delim("END LICENSE")
    val SIGNATURE_BEGIN = delim("BEGIN SIGNATURE")
    val SIGNATURE_END   = delim("END SIGNATURE")

    private def delim(s: String) = s"----- $s -----"
}
