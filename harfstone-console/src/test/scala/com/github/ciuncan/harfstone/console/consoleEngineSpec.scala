package com.github.ciuncan.harfstone.core.services

import com.github.ciuncan.harfstone.core.models.Card
import com.github.ciuncan.harfstone.core.models.Deck
import com.github.ciuncan.harfstone.core.models.Game
import com.github.ciuncan.harfstone.core.models.GameException
import com.github.ciuncan.harfstone.core.models.GameException.InvalidHandIndex
import com.github.ciuncan.harfstone.core.models.GameException.NotEnoughMana
import com.github.ciuncan.harfstone.core.models.GameResult
import com.github.ciuncan.harfstone.core.models.Player
import com.github.ciuncan.harfstone.core.models.PlayerTag
import com.github.ciuncan.harfstone.core.models.UserEvent
import com.github.ciuncan.harfstone.core.services.gameLogic.GameLogic

import com.github.ciuncan.harfstone.console.consoleEngine.ConsoleEngine
import com.github.ciuncan.harfstone.console.userInterface.ExitGameRequest
import com.github.ciuncan.harfstone.console.userInterface.UserInterface

import com.softwaremill.quicklens._

import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.mock.Expectation._
import zio.test.mock.mockable
import zio.test.mock.Expectation

object consoleEngineSpec extends DefaultRunnableSpec {

  /**
    * Produces mocked service objects for each method in UserInterface.Service, used to
    * build scenarios.
    *
    * @see https://zio.dev/docs/howto/howto_macros#description-1
    */
  @mockable[UserInterface.Service]
  object UserInterfaceMock

  /**
    * Produces mocked service objects for each method in GameLogic.Service, used to
    * build scenarios.
    *
    * @see https://zio.dev/docs/howto/howto_macros#description-1
    */
  @mockable[GameLogic.Service]
  object GameLogicMock

  val sampleGame = Game(
    Map(
      PlayerTag.First  -> Player(Deck.empty, Deck.empty, 30),
      PlayerTag.Second -> Player(Deck.empty, Deck.empty, 30)
    ),
    PlayerTag.First,
    1,
    1
  )

  val updatedGame = sampleGame.modify(_.turn).using(_ + 1)

  /**
    * Expectations can be implicitly converted to ZLayer which can provide its type argument, i.e.
    * dependencies of ConsoleEngine.Service.
    */
  type GameScenario = Expectation[GameLogic with UserInterface]

  def startGameWithScenario(gameScenario: GameScenario): IO[ExitGameRequest, Unit] =
    ConsoleEngine.startGame
      .provideLayer(gameScenario >>> ConsoleEngine.live)

  case class ScenarioTest(label: String, scenario: GameScenario, expectedResult: Either[ExitGameRequest, Unit]) {
    def toTest =
      testM(label)(
        for {
          result <- startGameWithScenario(scenario).either
        } yield assert(result)(equalTo(expectedResult))
      )
  }

  def incTurn(game: Game): Game = game.modify(_.turn).using(_ + 1)

  def gameStepScenario(
      currentGame: Game,
      transition: Game => Game,
      maybeWinner: Option[PlayerTag] = None
  ): GameScenario =
    UserInterfaceMock.PrintGame(equalTo(currentGame)) ++
      UserInterfaceMock.ReadEvent(value(UserEvent.PlayCard(0))) ++
      GameLogicMock.ProcessInteraction(
        equalTo((currentGame, UserEvent.PlayCard(0))),
        value(transition(currentGame))
      ) ++
      GameLogicMock.GetGameResult(
        equalTo(transition(currentGame)),
        value(maybeWinner.fold[GameResult](GameResult.Ongoing)(GameResult.Won(_)))
      )

  def gameStepWithExceptionScenario(currentGame: Game, gameException: GameException): GameScenario =
    UserInterfaceMock.PrintGame(equalTo(currentGame)) ++
      UserInterfaceMock.ReadEvent(value(UserEvent.PlayCard(0))) ++
      GameLogicMock.ProcessInteraction(
        equalTo((currentGame, UserEvent.PlayCard(0))),
        failure(gameException)
      )

  val scenarioTests: List[ScenarioTest] = List(
    ScenarioTest(
      label = "should initialize game, win at first round, print winner, ask another and exit on 'no'",
      scenario =
        GameLogicMock.InitializeGame(value(sampleGame)) ++
          gameStepScenario(sampleGame, incTurn, maybeWinner = Some(PlayerTag.First)) ++
          UserInterfaceMock.PrintWinner(equalTo(PlayerTag.First.asInstanceOf[PlayerTag])) ++
          UserInterfaceMock.AskAnotherGame(value(false)),
      expectedResult = Right(())
    ),
    ScenarioTest(
      label = "should win after two rounds, print winner, ask another and exit on 'no'",
      scenario =
        GameLogicMock.InitializeGame(value(sampleGame)) ++
          gameStepScenario(sampleGame, incTurn) ++
          gameStepScenario(sampleGame.copy(turn = 2), incTurn) ++
          gameStepScenario(sampleGame.copy(turn = 3), incTurn, maybeWinner = Some(PlayerTag.First)) ++
          UserInterfaceMock.PrintWinner(equalTo(PlayerTag.First.asInstanceOf[PlayerTag])) ++
          UserInterfaceMock.AskAnotherGame(value(false)),
      expectedResult = Right(())
    ),
    ScenarioTest(
      label = "should restart the game after one side winning, if ask another responded with 'yes'",
      scenario =
        GameLogicMock.InitializeGame(value(sampleGame)) ++
          gameStepScenario(sampleGame, incTurn, maybeWinner = Some(PlayerTag.Second)) ++
          UserInterfaceMock.PrintWinner(equalTo(PlayerTag.Second.asInstanceOf[PlayerTag])) ++
          UserInterfaceMock.AskAnotherGame(value(true)) ++

          GameLogicMock.InitializeGame(value(sampleGame)) ++
          gameStepScenario(sampleGame, incTurn, maybeWinner = Some(PlayerTag.First)) ++
          UserInterfaceMock.PrintWinner(equalTo(PlayerTag.First.asInstanceOf[PlayerTag])) ++
          UserInterfaceMock.AskAnotherGame(value(false)),
      expectedResult = Right(())
    ),
    ScenarioTest(
      label = "should exit the game if user doesn't provide input during a player interaction",
      scenario =
        GameLogicMock.InitializeGame(value(sampleGame)) ++
          UserInterfaceMock.PrintGame(equalTo(sampleGame)) ++
          UserInterfaceMock.ReadEvent(failure(ExitGameRequest())),
      expectedResult = Left(ExitGameRequest())
    ),
    ScenarioTest(
      label = "should exit the game if user doesn't provide input while asking for another game",
      scenario =
        GameLogicMock.InitializeGame(value(sampleGame)) ++
          gameStepScenario(sampleGame, incTurn, maybeWinner = Some(PlayerTag.First)) ++
          UserInterfaceMock.PrintWinner(equalTo(PlayerTag.First.asInstanceOf[PlayerTag])) ++
          UserInterfaceMock.AskAnotherGame(failure(ExitGameRequest())),
      expectedResult = Left(ExitGameRequest())
    ),
    ScenarioTest(
      label = "should handle the game exception during a player interaction, then continue with the same game state",
      scenario =
        GameLogicMock.InitializeGame(value(sampleGame)) ++

          gameStepWithExceptionScenario(sampleGame, InvalidHandIndex(deck = Deck.empty, index = 0)) ++
          UserInterfaceMock.HandleGameException(equalTo(InvalidHandIndex(Deck.empty, 0).asInstanceOf[GameException])) ++
          gameStepScenario(sampleGame, incTurn) ++

          gameStepWithExceptionScenario(sampleGame.copy(turn = 2), NotEnoughMana(card = Card(8), currentMana = 7)) ++
          UserInterfaceMock.HandleGameException(equalTo(NotEnoughMana(Card(8), 7).asInstanceOf[GameException])) ++
          gameStepScenario(sampleGame.copy(turn = 2), incTurn, maybeWinner = Some(PlayerTag.First)) ++

          UserInterfaceMock.PrintWinner(equalTo(PlayerTag.First.asInstanceOf[PlayerTag])) ++
          UserInterfaceMock.AskAnotherGame(value(false)),
      expectedResult = Right(())
    )
  )

  override def spec = suite("consoleEngine")(scenarioTests.map(_.toTest): _*)

}
