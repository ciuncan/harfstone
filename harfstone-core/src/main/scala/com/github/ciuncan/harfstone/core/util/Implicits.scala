package com.github.ciuncan.harfstone.core.util

import com.softwaremill.quicklens.PathModify

object Implicits {

  /**
    * Extension methods for a PathModify on U of T.
    *
    * @example
    * {{{
    *   case class X(a: Int)
    *   val x = X(5)
    *   val path: PathModify[X, Int] = x.modify(_.a)
    *   val newX = path.using(_ * 2)
    * }}}
    * @tparam T Source object type in this path
    * @tparam U Type of focused path in source object
    * @param path Receiver path to add methods on.
    * @see https://github.com/softwaremill/quicklens
    */
  implicit class RichPathModify[T, U](private val path: PathModify[T, U]) extends AnyVal {

    /**
      * Updates the value of U in this path using the base object T with given function.
      *
      * @example
      * {{{
      *   case class X(a: Int, b: String)
      *   val x = X(5, "hello")
      *   val newX = x.modify(_.b).usingBase(_.a.toString)
      *   newX shouldEqual X(5, "5")
      * }}}
      * @param mod Given function that returns U from base object T
      * @return Updated base object with given function
      */
    def usingBase(mod: T => U): T = path.doModify(path.obj, _ => mod(path.obj))

    /**
      * Updates the value of U in this path if given modification function returns Some.
      *
      * @example
      * {{{
      *   case class X(a: Int)
      *   def halfOfEven(a: Int): Option[Int] = if (a % 2 == 0) Some(a/2) else None
      *   X(5).modify(_.a).usingSome(halfOfEven) shouldEqual X(5)
      *   X(6).modify(_.a).usingSome(halfOfEven) shouldEqual X(3)
      * }}}
      * @param modOptional Given function that may return a modified value of U
      * @return Updated base object
      */
    def usingSome(modOptional: U => Option[U]): T = path.doModify(path.obj, u => modOptional(u).getOrElse(u))

    /**
      * Updates the value of U in this path if given modification function returns Right (non-error).
      *
      * @example
      * {{{
      *   case class X(a: Int)
      *   def halfOfEven(a: Int): Either[InvalidArgumentException, Int] =
      *     if (a % 2 == 0)
      *       Right(a/2)
      *     else
      *       Left(new InvalidArgumentException(s"$a isn't even"))
      *   X(5).modify(_.a).usingEither(halfOfEven) shouldEqual X(5)
      *   X(6).modify(_.a).usingEither(halfOfEven) shouldEqual X(3)
      * }}}
      * @param modEither Given function that may return either a modified value of U or any error
      * @return Updated base object
      */
    def usingRight(modEither: U => Either[Any, U]): T = usingSome(u => modEither(u).toOption)

  }

}
