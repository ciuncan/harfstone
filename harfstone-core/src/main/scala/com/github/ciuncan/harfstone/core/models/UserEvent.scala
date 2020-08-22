package com.github.ciuncan.harfstone.core.models

/**
  * Type of user interactions, can be end turn or playing a card by its index in hand.
  */
sealed trait UserEvent extends Product with Serializable
object UserEvent {
  case object EndTurn                       extends UserEvent
  final case class PlayCard(handIndex: Int) extends UserEvent
}
