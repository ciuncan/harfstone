package com.github.ciuncan.harfstone.core.models

import zio.test.Gen

object Generators {

  /**
    * Generates numbers between [1, 30]
    */
  val genSufficientHealth = Gen.int(1, 30)

  /**
    * Generates numbers between (-30,0]
    */
  val genInsufficientHealth = genSufficientHealth.map(_ - 30)

  val genPlayerTag = Gen.oneOf(
    Gen.const(PlayerTag.First),
    Gen.const(PlayerTag.Second)
  )

  val genCard = Gen.int(0, 8).map(Card(_))

  def genHandN(nCards: Int) =
    Gen.listOfN(nCards)(genCard)
      .map(_.to(Vector))
      .map(Deck(_))

  val genHand = Gen.int(0, 5).flatMap(genHandN)

  val genHandAndIndex = for {
    hand  <- genHand.filter(_.size > 0)
    index <- Gen.int(0, hand.size - 1)
  } yield (hand, index)

  val genHandAndInvalidIndex = for {
    hand  <- genHand
    index <- Gen.anyInt.map(_.abs).filter(_ >= hand.size)
  } yield (hand, index)

}
