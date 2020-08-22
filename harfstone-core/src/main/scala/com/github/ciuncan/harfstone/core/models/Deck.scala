package com.github.ciuncan.harfstone.core.models

import com.github.ciuncan.harfstone.core.models.GameException.InvalidHandIndex

import zio.URIO
import zio.random.Random

/**
  * Type that represents a set of cards (e.g. hand, deck).
  *
  * @param cards Cards of this deck
  */
final case class Deck(cards: Vector[Card]) {

  /**
    * Number of cards.
    */
  def size: Int = cards.size

  /**
    * Whether hand/deck is empty.
    */
  def isEmpty: Boolean = cards.isEmpty

  /**
    * @param index Given index of card in this deck
    * @return Card at the given index in Right if valid index,
    *         otherwise InvalidHandIndex in Left.
    */
  def cardAt(index: Int): Either[InvalidHandIndex, Card] =
    checkIndex(index).map(_ => cards(index))

  /**
    * @param index Given index of card in this deck
    * @return Deck without the card at the given index in Right if valid index,
    *         otherwise InvalidHandIndex in Left.
    */
  def removeAt(index: Int): Either[InvalidHandIndex, Deck] =
    checkIndex(index).map { _ =>
      val (left, right) = cards.splitAt(index)
      Deck(left ++ right.drop(1))
    }

  /**
    * @param card Given card
    * @return Deck with given given card on top of this deck.
    */
  def putTop(card: Card): Deck = Deck(card +: cards)

  /**
    * @return Maybe the card at the top if this deck is non-empty.
    */
  def top: Option[Card] = cards.headOption

  /**
    * @param card Given card
    * @return Number of cards that has the same mana cost as given card
    */
  def countOf(card: Card): Int = cards.count(_ == card)

  private def checkIndex(index: Int): Either[InvalidHandIndex, ()] =
    if (0 <= index && index < size)
      Right(())
    else
      Left(InvalidHandIndex(this, index))

}

object Deck {

  /**
    * Empty deck, usually used for initializing hand.
    */
  def empty: Deck = Deck(Vector.empty)

  /**
    * Default deck with initial cards.
    */
  def default: Deck =
    Deck(
      Vector(0, 0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 7, 8)
        .map(Card(_))
    )

  /**
    * Create deck of cards comprised of given mana costs.
    *
    * @param costs Given sequence of mana costs
    * @return Deck with cards of given costs
    */
  def byManaCosts(costs: Int*): Deck = Deck(costs.map(Card(_)).to(Vector))

  /**
    * Effect of getting a shuffled deck that depends on Random service.
    */
  def shuffled: URIO[Random, Deck] =
    URIO.accessM[Random](_.get.shuffle(default.cards))
      .map(Deck(_))

}
