package com.github.ciuncan.harfstone.console

import com.github.ciuncan.harfstone.core.models.Game
import com.github.ciuncan.harfstone.core.models.GameResult
import com.github.ciuncan.harfstone.core.services.gameLogic.GameLogic

import com.github.ciuncan.harfstone.console.userInterface.ExitGameRequest
import com.github.ciuncan.harfstone.console.userInterface.UserInterface

import zio._
import zio.macros.accessible

/**
  * Object encapsulating ConsoleEngine service and helper functions.
  */
object consoleEngine {

  /**
    * Dependency declaration type of ConsoleEngine.Service
    */
  type ConsoleEngine = Has[ConsoleEngine.Service]

  @accessible
  object ConsoleEngine {

    /**
      * ConsoleEngine service.
      */
    trait Service {

      /**
        * Effect of starting the console game, may fail with ExitGameRequest.
        */
      def startGame: IO[ExitGameRequest, Unit]
    }

    /**
      * Live implementation of ConsoleEngine service that depends on GameLogic and UserInterface.
      */
    val live: URLayer[GameLogic with UserInterface, ConsoleEngine] =
      ZLayer.fromServices[GameLogic.Service, UserInterface.Service, ConsoleEngine.Service]((gameLogic, userInterface) =>
        new ConsoleEngine.Service {

          def startGame: IO[ExitGameRequest, Unit] =
            for {
              _     <- newGame
              isYes <- userInterface.askAnotherGame
              _     <- if (isYes)
                         startGame
                       else
                         IO.succeed(())
            } yield ()

          def gameLoop(game: Game): IO[ExitGameRequest, Game] =
            for {
              _         <- userInterface.printGame(game)
              event     <- userInterface.readEvent
              finalGame <- gameLogic.processInteraction(game, event).foldM(
                             gameExc => userInterface.handleGameException(gameExc) *> gameLoop(game),
                             procGame =>
                               gameLogic.getGameResult(procGame).flatMap({
                                 case GameResult.Ongoing        => gameLoop(procGame)
                                 case GameResult.Won(playerTag) =>
                                   userInterface.printWinner(playerTag) *> UIO.succeed(procGame)
                               })
                           )
            } yield finalGame

          private def newGame: IO[ExitGameRequest, Unit] =
            for {
              initGame <- gameLogic.initializeGame
              _        <- gameLoop(initGame)
            } yield ()

        }
      )

  }

}
