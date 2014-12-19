package org.distfp.sts

object Delim {
    val defaultTextMarker = "LICENSE"
    val signatureMarker   = "SIGNATURE"

    val delimSeparator = "-----"

    def beginDelim(marker: String) = delim("BEGIN " + marker)
    def endDelim(marker: String) = delim("END" + marker)

    private def delim(s: String) = s"$delimSeparator $s $delimSeparator"

    val signatureBegin = beginDelim(signatureMarker)
    val signatureEnd   = endDelim(signatureMarker)
}
