package com.github.ciuncan.harfstone.core.services

import com.github.ciuncan.harfstone.core.models.Deck
import com.github.ciuncan.harfstone.core.models.Game
import com.github.ciuncan.harfstone.core.models.GameException
import com.github.ciuncan.harfstone.core.models.GameException.NotEnoughMana
import com.github.ciuncan.harfstone.core.models.GameResult
import com.github.ciuncan.harfstone.core.models.PlayerTag
import com.github.ciuncan.harfstone.core.models.Player
import com.github.ciuncan.harfstone.core.models.UserEvent

import com.github.ciuncan.harfstone.core.util.Implicits.RichPathModify

import com.softwaremill.quicklens._
import zio._
import zio.random.Random
import zio.macros.accessible

/**
  * Object encapsulating GameLogic service and helper functions.
  */
package object gameLogic {

  /**
    * Dependency declaration type of GameLogic.Service
    */
  type GameLogic = Has[GameLogic.Service]

  @accessible
  object GameLogic {

    /**
      * GameLogic service.
      */
    trait Service {

      /**
        * Infallible effect of initializing a new game state.
        */
      def initializeGame: UIO[Game]

      /**
        * Effect of processing given game state with given user interaction event.
        *
        * @param game Given game state
        * @param event Given user interaction event
        * @return Effect with new game state or a game exception
        */
      def processInteraction(game: Game, event: UserEvent): IO[GameException, Game]

      /**
        * Effect of calculating game result of given game state.
        *
        * @param game Given game state
        */
      def getGameResult(game: Game): UIO[GameResult]
    }

    /**
      * Live implementation of GameLogic service that depends on Random service.
      */
    val live: ZLayer[Random, Nothing, GameLogic] = ZLayer.fromFunction(random =>
      new GameLogic.Service {

        def initializeGame: UIO[Game] = {
          def draw3Cards(player: Player): Player = LazyList.iterate(player)(drawTopCard).drop(3).head

          for {
            player1Deck <- Deck.shuffled.provide(random)
            player2Deck <- Deck.shuffled.provide(random)
            player1      = draw3Cards(Player(player1Deck, hand = Deck.empty, currentHealth = 30))
            player2      = draw3Cards(Player(player2Deck, hand = Deck.empty, currentHealth = 30))
          } yield Game(
            turn = 1,
            currentMana = 1,
            currentPlayerTag = PlayerTag.First,
            players = Map(
              PlayerTag.First  -> player1,
              PlayerTag.Second -> player2
            )
          )
        }

        def processInteraction(game: Game, event: UserEvent): IO[GameException, Game] =
          event match {
            case UserEvent.EndTurn             => switchTurn(game)
            case UserEvent.PlayCard(handIndex) => playCard(game, handIndex)
          }

        def getGameResult(game: Game): UIO[GameResult] =
          UIO.succeed {
            if (game.players(game.currentPlayerTag.other).currentHealth <= 0)
              GameResult.Won(game.currentPlayerTag)
            else
              GameResult.Ongoing
          }

        private def switchTurn(game: Game): UIO[Game] =
          UIO.succeed {
            val newPlayerTag = game.currentPlayerTag.other

            game
              .modify(_.turn).usingIf(game.currentPlayerTag == PlayerTag.Second)(_ + 1)
              .modify(_.currentMana).usingBase(g => if (g.turn <= 10) g.turn else 10)
              .modify(_.currentPlayerTag).setTo(newPlayerTag)
              .modify(_.players.at(newPlayerTag)).using(drawTopCard)
          }

        private def playCard(game: Game, handIndex: Int): IO[GameException, Game] = {
          val currentPlayer = game.players(game.currentPlayerTag)
          for {
            playedCard <- IO.fromEither(currentPlayer.hand.cardAt(handIndex))
            newHand    <- IO.fromEither(currentPlayer.hand.removeAt(handIndex))
            _          <- IO.when(game.currentMana < playedCard.manaCost) {
                            IO.fail(NotEnoughMana(playedCard, game.currentMana))
                          }
          } yield game
            .modify(_.players.at(game.currentPlayerTag).hand).setTo(newHand)
            .modify(_.players.at(game.currentPlayerTag.other).currentHealth).using(_ - playedCard.dmg)
            .modify(_.currentMana).using(_ - playedCard.manaCost)
        }

        private def drawTopCard(player: Player): Player = {
          // Tries to take a card from deck and put into hand. It will discard the
          // the drawn card if hand has more than maximum number of cards. If there
          // are no more cards left in deck then player will bleed, i.e. reduce the
          // health by 1.

          val shouldBleed = player.deck.isEmpty
          val canDraw     = !shouldBleed && player.hand.size < 5

          player
            .modify(_.currentHealth).usingIf(shouldBleed)(_ - 1)
            .modify(_.deck).usingRight(_.removeAt(0))
            .modify(_.hand).usingIf(canDraw)(_.putTop(player.deck.top.get))
        }
      }
    )

  }

}
