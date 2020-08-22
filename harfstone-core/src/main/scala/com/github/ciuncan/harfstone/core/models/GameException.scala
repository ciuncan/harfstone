package com.github.ciuncan.harfstone.core.models

/**
  * Possible exceptions of this game.
  *
  * @param msg Message of this exception
  */
sealed abstract class GameException(msg: String) extends RuntimeException(msg) with Product with Serializable
object GameException {

  /**
    * Invalid hand index for a given hand.
    * @param deck Given hand
    * @param index The invalid index w.r.t. given given
    */
  final case class InvalidHandIndex(deck: Deck, index: Int)
      extends GameException(s"Invalid index=$index for deck=$deck")

  /**
    * Insufficient mana to use given card.
    *
    * @param card Given card attempted to use
    * @param currentMana Current mana available to the player
    */
  final case class NotEnoughMana(card: Card, currentMana: Int)
      extends GameException(s"Not enough mana=$currentMana to play card=$card")
}
