package com.github.ciuncan.harfstone.core.models

import com.github.ciuncan.harfstone.core.util.EnumBase

/**
  * Tag of a player, can be either First or Second.
  */
sealed trait PlayerTag extends EnumBase[PlayerTag] {

  /**
    * Other player than this one.
    */
  def other: PlayerTag

  /**
    * @return 1 if this is First, otherwise 2
    */
  def num: Int
}
object PlayerTag {
  case object First  extends PlayerTag {
    def other = Second
    def num   = 1
  }
  case object Second extends PlayerTag {
    def other = First
    def num   = 2
  }
}
