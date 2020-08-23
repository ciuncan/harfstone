package com.github.ciuncan.harfstone.console

import com.github.ciuncan.harfstone.core.models.Card
import com.github.ciuncan.harfstone.core.models.Game
import com.github.ciuncan.harfstone.core.models.GameException
import com.github.ciuncan.harfstone.core.models.GameException.InvalidHandIndex
import com.github.ciuncan.harfstone.core.models.GameException.NotEnoughMana
import com.github.ciuncan.harfstone.core.models.PlayerTag
import com.github.ciuncan.harfstone.core.models.UserEvent

import zio._
import zio.console.Console
import zio.macros.accessible

import scala.io.{AnsiColor => AC}

/**
  * Object encapsulating UserInterface service and helper functions.
  */
object userInterface {

  /**
    * Dependency declaration type of UserInterface.Service
    */
  type UserInterface = Has[UserInterface.Service]

  /**
    * Error type denoting user wanting to exit application, should be handled at top level.
    */
  final case class ExitGameRequest() extends RuntimeException

  @accessible
  object UserInterface {

    /**
      * UserInterface service.
      */
    trait Service {

      /**
        * Infallible effect of printing given game state.
        *
        * @param game Given game
        */
      def printGame(game: Game): UIO[Unit]

      /**
        * Infallible effect of printing the given player tag as winner.
        *
        * @param playerTag Given player tag
        */
      def printWinner(playerTag: PlayerTag): UIO[Unit]

      /**
        * Effect of asking for user input whether he wants to continue or not (continually until valid),
        * may fail with ExitGameRequest.
        *
        * @return true, when run, if user wants to continue, false otherwise
        */
      def askAnotherGame: IO[ExitGameRequest, Boolean]

      /**
        * Effect of asking for user interaction (continually until valid), may fail with ExitGameRequest.
        *
        * @return When run, UserEvent.EndTurn if "end" is entered, UserEvent.PlayCard if index entered
        */
      def readEvent: IO[ExitGameRequest, UserEvent]

      /**
        * Infallible effect of handling a game exception (InvalidHandIndex or NotEnoughMana).
        *
        * @param e Given game exception to handle
        */
      def handleGameException(e: GameException): UIO[Unit]
    }

    /**
      * Live implementation of UserInterface service that depends on Console service.
      */
    val live: ZLayer[Console, Nothing, UserInterface] = ZLayer.fromService(console =>
      new UserInterface.Service {

        def printWinner(playerTag: PlayerTag): UIO[Unit] =
          console.putStr("Winner is ") *> printPlayerTag(playerTag) *> console.putStrLn("!")

        def handleGameException(e: GameException): UIO[Unit] =
          console.putStrLn(e match {
            case InvalidHandIndex(hand, index)    =>
              s"$index is invalid, please enter a number between 0 and ${hand.size - 1}."
            case NotEnoughMana(card, currentMana) =>
              s"Not enough mana to play card $card, you only have $currentMana."
          })

        def readEvent: IO[ExitGameRequest, UserEvent] =
          for {
            _     <- console.putStrLn("Enter a card index to play, or 'end' to end turn:")
            input <- getInput.flatMap({
                       case s if s.toIntOption.isDefined => UIO.succeed(UserEvent.PlayCard(s.toInt))
                       case "end"                        => UIO.succeed(UserEvent.EndTurn)
                       case unknown                      => console.putStr(s"'$unknown' not understood, ") *> readEvent
                     })
          } yield input

        def askAnotherGame: IO[ExitGameRequest, Boolean] =
          console.putStrLn("Play another game?") *> getInput
            .flatMap {
              case "yes" => UIO.succeed(true)
              case "no"  => UIO.succeed(false)
              case s     => console.putStr(s"'$s' not understood, please answer yes or no. ") *> askAnotherGame
            }

        def printGame(game: Game): UIO[Unit] =
          for {
            _ <- newLine.repeatN(20)
            _ <- console.putStrLn("Turn: " + game.turn) *> newLine
            _ <- console.putStr("Current player: ") *> printPlayerTag(game.currentPlayerTag) *> newLine
            _ <- console.putStr("Current mana:   ") *>
                   putStrCol(AC.BLUE_B + AC.WHITE)(game.currentMana.toString) *>
                   newLine
            _ <- newLine
            _ <- printPlayer(PlayerTag.First).provide(game) *> newLine
            _ <- printPlayer(PlayerTag.Second).provide(game) *> newLine
          } yield ()

        private def printPlayer(playerTag: PlayerTag): URIO[Game, Unit] =
          for {
            player <- ZIO.access[Game](_.players(playerTag))
            _      <- console.putStrLn(s"Player${playerTag.num} Health: ${player.currentHealth}")
            _      <- printHand(playerTag)
            _      <- console.putStrLn(s"Player${playerTag.num} Deck has ${player.deck.size} cards")
          } yield ()

        private def printHand(playerTag: PlayerTag): URIO[Game, Unit] =
          for {
            handCards <- ZIO.access[Game](_.players(playerTag).hand.cards)

            header = s"Player${playerTag.num} Cards: "

            _ <- console.putStr(header)
            _ <- ZIO.foreach_(handCards.indices)(i => console.putStr(f"$i%3d")) *> newLine

            _ <- console.putStr(" " * header.length)
            _ <- ZIO.foreach_(handCards)(printCard(playerTag)) *> newLine
          } yield ()

        private def printCard(owner: PlayerTag)(card: Card): URIO[Game, Unit] =
          for {
            currentMana   <- ZIO.access[Game](_.currentMana)
            currentPlayer <- ZIO.access[Game](_.currentPlayerTag)
            color          = if (owner != currentPlayer)
                               ""
                             else if (card.manaCost <= currentMana)
                               AC.GREEN
                             else
                               AC.RED
            _             <- putStrCol(color)(f"${card.manaCost}%3d")
          } yield ()

        private def printPlayerTag(playerTag: PlayerTag): UIO[Unit] = {
          val color =
            if (playerTag == PlayerTag.First)
              AC.CYAN
            else
              AC.MAGENTA
          putStrCol(AC.BOLD + color)(playerTag.toString)
        }

        private def getInput: IO[ExitGameRequest, String] =
          console.getStrLn
            .map(_.trim.toLowerCase)
            .mapError(_ => ExitGameRequest())

        private def putStrCol(ansiStyle: String)(text: String): UIO[Unit] =
          console.putStr(ansiStyle + text + (if (ansiStyle.isEmpty) "" else AC.RESET))

        private def newLine = console.putStrLn("")
      }
    )

  }

}
