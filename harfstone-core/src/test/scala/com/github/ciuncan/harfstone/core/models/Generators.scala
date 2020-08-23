package com.github.ciuncan.harfstone.core.models

import zio.test.Gen
import zio.random.Random

object Generators {

  /**
    * Generates numbers between [1, 30]
    */
  val genSufficientHealth: Gen[Random, Int] = Gen.int(1, 30)

  /**
    * Generates numbers between (-30,0]
    */
  val genInsufficientHealth: Gen[Random, Int] = genSufficientHealth.map(_ - 30)

  val genPlayerTag: Gen[Random, PlayerTag] = Gen.oneOf(
    Gen.const(PlayerTag.First),
    Gen.const(PlayerTag.Second)
  )

  val genCard: Gen[Random, Card] = Gen.int(0, 8).map(Card(_))

  def genHandN(nCards: Int): Gen[Random, Deck] =
    Gen.listOfN(nCards)(genCard)
      .map(_.to(Vector))
      .map(Deck(_))

  val genHand: Gen[Random, Deck] = Gen.int(0, 5).flatMap(genHandN)

  val genHandAndIndex: Gen[Random, (Deck, Int)] = for {
    hand  <- genHand.filter(_.size > 0)
    index <- Gen.int(0, hand.size - 1)
  } yield (hand, index)

  val genHandAndInvalidIndex: Gen[Random, (Deck, Int)] = for {
    hand  <- genHand
    index <- Gen.anyInt.map(_.abs).filter(_ >= hand.size)
  } yield (hand, index)

}
