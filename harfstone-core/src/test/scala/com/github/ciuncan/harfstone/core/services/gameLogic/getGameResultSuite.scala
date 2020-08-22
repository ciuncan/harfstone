package com.github.ciuncan.harfstone.core.services.gameLogic

import com.github.ciuncan.harfstone.core.models.GameResult
import com.github.ciuncan.harfstone.core.models.Generators
import com.github.ciuncan.harfstone.core.models.PlayerTag

import com.softwaremill.quicklens._
import zio.test._
import zio.test.Assertion._
import zio.test.BoolAlgebra.all

object getGameResultSuite extends DefaultRunnableSpec {

  import GameLogic.initializeGame
  import GameLogic.getGameResult

  import Generators.genSufficientHealth
  import Generators.genInsufficientHealth

  override def spec =
    suite("gameLogic.getGameResult")(
      testM("should return ongoing if both player has health > 0")(
        checkM(genSufficientHealth, genSufficientHealth)((p1Health, p2Health) =>
          for {
            game   <- initializeGame
            modGame = game
                        .modify(_.players.at(PlayerTag.First).currentHealth).setTo(p1Health)
                        .modify(_.players.at(PlayerTag.Second).currentHealth).setTo(p2Health)

            result <- getGameResult(modGame)
          } yield assert(result)(equalTo(GameResult.Ongoing))
        )
      ),
      testM("should return current player win if other player has health <= 0")(
        checkM(genSufficientHealth, genInsufficientHealth)((sufficientHealth, insufficientHealth) =>
          for {
            game    <- initializeGame
            newGame1 = game
                         .modify(_.players.at(PlayerTag.First).currentHealth).setTo(sufficientHealth)
                         .modify(_.players.at(PlayerTag.Second).currentHealth).setTo(insufficientHealth)
                         .modify(_.currentPlayerTag).setTo(PlayerTag.First)
            newGame2 = game
                         .modify(_.players.at(PlayerTag.First).currentHealth).setTo(insufficientHealth)
                         .modify(_.players.at(PlayerTag.Second).currentHealth).setTo(sufficientHealth)
                         .modify(_.currentPlayerTag).setTo(PlayerTag.Second)

            result1 <- getGameResult(newGame1)
            result2 <- getGameResult(newGame2)
          } yield all(
            assert(result1)(equalTo(GameResult.Won(PlayerTag.First))),
            assert(result2)(equalTo(GameResult.Won(PlayerTag.Second)))
          )
        )
      )
    ).provideCustomLayer(GameLogic.live)

}
