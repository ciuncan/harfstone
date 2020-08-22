package com.github.ciuncan.harfstone.core.util

import com.github.ciuncan.harfstone.core.util.Implicits.RichPathModify

import com.softwaremill.quicklens._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ImplicitsSpec extends AnyFlatSpec with Matchers {

  case class X(a: Int, b: String)
  case class Y(a: Int)

  "RichPathModify" should "modify focused part using other parts of base object" in {
    X(5, "hello").modify(_.b).usingBase(_.a.toString) shouldEqual X(5, "5")
  }

  it should "modify focused part with a function that returns Option if it is Some" in {
    def halfOfEven(a: Int): Option[Int] = if (a % 2 == 0) Some(a / 2) else None

    Y(5).modify(_.a).usingSome(halfOfEven) shouldEqual Y(5)
    Y(6).modify(_.a).usingSome(halfOfEven) shouldEqual Y(3)
  }

  it should "modify focused part with a function that returns Either if it is Right" in {
    def halfOfEven(a: Int): Either[IllegalArgumentException, Int] =
      if (a % 2 == 0)
        Right(a / 2)
      else
        Left(new IllegalArgumentException(s"$a isn't even"))

    Y(5).modify(_.a).usingRight(halfOfEven) shouldEqual Y(5)
    Y(6).modify(_.a).usingRight(halfOfEven) shouldEqual Y(3)
  }

}
