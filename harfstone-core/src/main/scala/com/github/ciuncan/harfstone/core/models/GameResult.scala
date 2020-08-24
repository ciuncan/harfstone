package com.github.ciuncan.harfstone.core.models

import com.github.ciuncan.harfstone.core.util.EnumBase

/**
  * Result of a game, either a winner with tag or ongoing.
  */
sealed trait GameResult extends EnumBase[GameResult]
object GameResult {
  case object Ongoing                     extends GameResult
  final case class Won(winner: PlayerTag) extends GameResult
}
