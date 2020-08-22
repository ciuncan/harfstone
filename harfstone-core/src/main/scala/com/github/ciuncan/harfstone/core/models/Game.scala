package com.github.ciuncan.harfstone.core.models

/**
  * Type of a game state structure.
  *
  * @param players          Mapping of tags to player structures
  * @param currentPlayerTag Tag of player that has the turn
  * @param turn             Number of turns so far
  * @param currentMana      Remaining mana of current player
  */
final case class Game(players: Map[PlayerTag, Player], currentPlayerTag: PlayerTag, turn: Int, currentMana: Int)
