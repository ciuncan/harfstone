package com.github.ciuncan.harfstone.core.util

/**
  * Base trait for sum types.
  */
trait EnumBase[T <: EnumBase[T]] extends Product with Serializable {

  /**
    * Converts specific variant type to base a base type in enum hierarchy.
    */
  def widen[U >: EnumBase[T]]: U = this
}
