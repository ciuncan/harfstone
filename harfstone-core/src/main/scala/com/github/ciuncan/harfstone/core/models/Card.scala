package com.github.ciuncan.harfstone.core.models

/**
  * Type of a card.
  *
  * @param manaCost Mana cost of this card
  */
final case class Card(manaCost: Int) {

  /**
    * Damage of this card, equal to its mana cost.
    */
  def dmg: Int = manaCost
}
object Card {
  implicit val cardOrderable: Ordering[Card] = Ordering.by(_.manaCost)
}
