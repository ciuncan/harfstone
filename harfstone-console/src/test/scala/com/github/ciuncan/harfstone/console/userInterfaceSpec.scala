package com.github.ciuncan.harfstone.core.services

import com.github.ciuncan.harfstone.core.models.Game
import com.github.ciuncan.harfstone.core.models.GameException.InvalidHandIndex
import com.github.ciuncan.harfstone.core.models.GameException.NotEnoughMana
import com.github.ciuncan.harfstone.core.models.Generators
import com.github.ciuncan.harfstone.core.models.PlayerTag
import com.github.ciuncan.harfstone.core.models.Player
import com.github.ciuncan.harfstone.core.models.Card
import com.github.ciuncan.harfstone.core.models.Deck
import com.github.ciuncan.harfstone.core.models.UserEvent

import com.github.ciuncan.harfstone.console.userInterface.ExitGameRequest
import com.github.ciuncan.harfstone.console.userInterface.UserInterface

import zio.test._
import zio.test.Assertion._
import zio.test.BoolAlgebra.all
import zio.test.environment._

import scala.io.{AnsiColor => AC}

object userInterfaceSpec extends DefaultRunnableSpec {

  import UserInterface.printGame
  import UserInterface.printWinner
  import UserInterface.askAnotherGame
  import UserInterface.readEvent
  import UserInterface.handleGameException

  import Generators.genCard
  import Generators.genHandAndInvalidIndex

  override def spec =
    suite("userInterface")(
      testM("should print winner color according to player tag")(
        for {
          _      <- TestConsole.clearOutput
          _      <- printWinner(PlayerTag.First)
          _      <- printWinner(PlayerTag.Second)
          output <- TestConsole.output
        } yield assert(output.mkString)(equalTo(
          s"""Winner is ${AC.BOLD + AC.CYAN}First${AC.RESET}!
             |Winner is ${AC.BOLD + AC.MAGENTA}Second${AC.RESET}!
             |""".stripMargin
        ))
      ),
      testM("should handle game exception by printing it")(
        checkM(genHandAndInvalidIndex, genCard, Gen.int(1, 8)) {
          case ((hand, index), card, currentMana) =>
            for {
              _      <- TestConsole.clearOutput
              _      <- handleGameException(InvalidHandIndex(hand, index))
              _      <- handleGameException(NotEnoughMana(card, currentMana))
              output <- TestConsole.output
            } yield assert(output)(equalTo(Vector(
              s"${InvalidHandIndex(hand, index).getMessage}\n",
              s"${NotEnoughMana(card, currentMana).getMessage}\n"
            )))
        }
      ),
      suite("readEvent")(
        testM("should ask to enter interaction")(
          for {
            _          <- TestConsole.clearInput
            _          <- TestConsole.feedLines("1")
            readResult <- readEvent.either
            output     <- TestConsole.output
          } yield assert(output)(equalTo(Vector(
            "Enter a card index to play, or 'end' to end turn:\n"
          )))
        ),
        testM("should fail with ExitGameRequest if input is empty")(
          for {
            _          <- TestConsole.clearInput
            readResult <- readEvent.either
          } yield assert(readResult)(equalTo(Left(ExitGameRequest())))
        ),
        testM("should return PlayCard with input index, if a number is entered")(
          for {
            _          <- TestConsole.clearInput
            _          <- TestConsole.feedLines("  1 ")
            readResult <- readEvent.either
          } yield assert(readResult)(equalTo(Right(UserEvent.PlayCard(1))))
        ),
        testM("should return EndTurn if 'end' is entered")(
          for {
            _          <- TestConsole.clearInput
            _          <- TestConsole.feedLines(" end   ")
            readResult <- readEvent.either
          } yield assert(readResult)(equalTo(Right(UserEvent.EndTurn)))
        ),
        testM("should keep asking until a valid input is entered")(
          for {
            _          <- TestConsole.clearInput
            _          <- TestConsole.clearOutput
            _          <- TestConsole.feedLines("x", " y ", "end")
            readResult <- readEvent.either
            output     <- TestConsole.output
          } yield all(
            assert(readResult)(equalTo(Right(UserEvent.EndTurn))),
            assert(output.mkString)(equalTo(
              s"""Enter a card index to play, or 'end' to end turn:
                 |'x' not understood, Enter a card index to play, or 'end' to end turn:
                 |'y' not understood, Enter a card index to play, or 'end' to end turn:
                 |""".stripMargin
            ))
          )
        )
      ),
      suite("askAnotherGame")(
        testM("should ask to enter interaction")(
          for {
            _          <- TestConsole.clearInput
            _          <- TestConsole.feedLines("yes")
            readResult <- askAnotherGame.either
            output     <- TestConsole.output
          } yield assert(output)(equalTo(Vector(
            "Play another game?\n"
          )))
        ),
        testM("should fail with ExitGameRequest if input is empty")(
          for {
            _          <- TestConsole.clearInput
            readResult <- askAnotherGame.either
          } yield assert(readResult)(equalTo(Left(ExitGameRequest())))
        ),
        testM("should return true if yes and false if no is entered")(
          for {
            _           <- TestConsole.clearInput
            _           <- TestConsole.feedLines("  yes ", "  No ")
            readResult1 <- askAnotherGame.either
            readResult2 <- askAnotherGame.either
          } yield all(
            assert(readResult1)(equalTo(Right(true))),
            assert(readResult2)(equalTo(Right(false)))
          )
        ),
        testM("should keep asking until a valid input is entered")(
          for {
            _          <- TestConsole.clearInput
            _          <- TestConsole.clearOutput
            _          <- TestConsole.feedLines("x", " y ", "yes")
            readResult <- askAnotherGame.either
            output     <- TestConsole.output
          } yield all(
            assert(readResult)(equalTo(Right(true))),
            assert(output.mkString)(equalTo(
              s"""Play another game?
                 |'x' not understood, please answer yes or no. Play another game?
                 |'y' not understood, please answer yes or no. Play another game?
                 |""".stripMargin
            ))
          )
        )
      ),
      testM("should print the game state, highlighting affordable cards with green order with red for current user")(
        for {
          _      <- TestConsole.clearOutput
          _      <- printGame(sampleGame)
          output <- TestConsole.output
        } yield {
          val expected = s"""
             |Turn: 6
             |
             |Current player: ${AC.BOLD + AC.CYAN}First${AC.RESET}
             |Current mana:   ${AC.BLUE_B + AC.WHITE}6${AC.RESET}
             |
             |Player1 Health: 19
             |Player1 Cards:   0  1  2
             |               ${AC.RED}  7${AC.RESET}${AC.GREEN}  1${AC.RESET}${AC.GREEN}  6${AC.RESET}
             |Player1 Deck has 1 cards
             |
             |Player2 Health: 14
             |Player2 Cards:   0  1  2
             |                 8  2  5
             |Player2 Deck has 2 cards
             |""".stripMargin.trim

          val actual = output.mkString.trim

          val diff = actual.zip(expected).zipWithIndex.filter({ case ((a, e), _) => a != e }).toArray
          println(s"actual.length=${actual.length} expected.length=${expected.length}\n$diff")
          assert(actual)(equalTo(expected))
        }
      )
    )
      .provideCustomLayer(UserInterface.live)

  val sampleGame = Game(
    players = Map(
      PlayerTag.First  -> Player(
        deck = Deck(Vector(1).map(Card(_))),
        hand = Deck(Vector(7, 1, 6).map(Card(_))),
        currentHealth = 19
      ),
      PlayerTag.Second -> Player(
        deck = Deck(Vector(2, 4).map(Card(_))),
        hand = Deck(Vector(8, 2, 5).map(Card(_))),
        currentHealth = 14
      )
    ),
    currentMana = 6,
    currentPlayerTag = PlayerTag.First,
    turn = 6
  )

}
