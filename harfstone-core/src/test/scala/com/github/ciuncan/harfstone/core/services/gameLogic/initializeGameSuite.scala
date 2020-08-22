package com.github.ciuncan.harfstone.core.services.gameLogic

import com.github.ciuncan.harfstone.core.models.PlayerTag

import zio.test._
import zio.test.Assertion._
import zio.test.BoolAlgebra.all
import zio.test.TestAspect.flaky

object initializeGameSuite extends DefaultRunnableSpec {

  import GameLogic.initializeGame

  override def spec =
    suite("gameLogic.initializeGame")(
      testM("should create players with 30 health")(
        for {
          game   <- initializeGame
          player1 = game.players(PlayerTag.First)
          player2 = game.players(PlayerTag.Second)
        } yield all(
          assert(player1.currentHealth)(equalTo(30)),
          assert(player2.currentHealth)(equalTo(30))
        )
      ),
      testM("should create decks shuffled")(
        for {
          game   <- initializeGame
          player1 = game.players(PlayerTag.First)
          player2 = game.players(PlayerTag.Second)
        } yield all(
          assert(player1.deck.cards)(not(equalTo(player1.deck.cards.sorted))),
          assert(player2.deck.cards)(not(equalTo(player2.deck.cards.sorted)))
        )
      ) @@ flaky,
      testM("should start the game with 1 mana")(
        for {
          game <- initializeGame
        } yield assert(game.currentMana)(equalTo(1))
      ),
      testM("should start the game 3 cards drawn from deck into hand for each player")(
        for {
          game   <- initializeGame
          player1 = game.players(PlayerTag.First)
          player2 = game.players(PlayerTag.Second)
        } yield all(
          assert(player1.hand.size)(equalTo(3)),
          assert(player2.hand.size)(equalTo(3)),
          assert(player1.deck.size)(equalTo(17)),
          assert(player1.deck.size)(equalTo(17))
        )
      )
    ).provideCustomLayer(GameLogic.live)

}
