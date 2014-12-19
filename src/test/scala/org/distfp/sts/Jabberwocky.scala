package org.distfp.sts

object Jabberwocky {
  def apply(i: Int) = paragraphs(i)

  lazy val all = paragraphs.reduceLeft(_ ++ _)

  val paragraphs = Array(
      Array(
        "Twas brillig, and the slithy toves",
        "Did gyre and gimble in the wabe;",
        "All mimsy were the borogoves,",
        "And the mome raths outgrabe."),

      Array(
        "Beware the Jabberwock, my son!",
        "The jaws that bite, the claws that catch!",
        "Beware the Jubjub bird, and shun",
        "The frumious Bandersnatch!"),

      Array(
        "He took his vorpal sword in hand:",
        "Long time the manxome foe he sought—",
        "So rested he by the Tumtum tree,",
        "And stood awhile in thought")
  )
}
