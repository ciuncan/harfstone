package com.github.ciuncan.harfstone.core.models

/**
  * Result of a game, either a winner with tag or ongoing.
  */
sealed trait GameResult extends Product with Serializable
object GameResult {
  case object Ongoing                     extends GameResult
  final case class Won(winner: PlayerTag) extends GameResult
}
