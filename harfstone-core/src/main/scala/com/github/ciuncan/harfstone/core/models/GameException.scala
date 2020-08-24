package com.github.ciuncan.harfstone.core.models

import com.github.ciuncan.harfstone.core.util.EnumBase

/**
  * Possible exceptions of during player interaction.
  */
sealed trait GameException extends Throwable with EnumBase[GameException]
object GameException {

  /**
    * Invalid hand index for a given hand.
    * @param hand Given hand
    * @param index The invalid index w.r.t. given hand
    */
  final case class InvalidHandIndex(hand: Deck, index: Int) extends GameException

  /**
    * Insufficient mana to use given card.
    *
    * @param card Given card attempted to use
    * @param currentMana Current mana available to the player
    */
  final case class NotEnoughMana(card: Card, currentMana: Int) extends GameException
}
