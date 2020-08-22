package com.github.ciuncan.harfstone.core.models

/**
  * Type of a player structure
  *
  * @param deck Current remaining cards in the deck
  * @param hand Current cards in the hand
  * @param currentHealth Current remaining health
  */
final case class Player(deck: Deck, hand: Deck, currentHealth: Int)
