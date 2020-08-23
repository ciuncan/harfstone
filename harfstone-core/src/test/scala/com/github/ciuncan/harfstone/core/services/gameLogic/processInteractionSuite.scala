package com.github.ciuncan.harfstone.core.services.gameLogic

import com.github.ciuncan.harfstone.core.models.Deck
import com.github.ciuncan.harfstone.core.models.GameException.NotEnoughMana
import com.github.ciuncan.harfstone.core.models.GameException.InvalidHandIndex
import com.github.ciuncan.harfstone.core.models.Generators
import com.github.ciuncan.harfstone.core.models.PlayerTag
import com.github.ciuncan.harfstone.core.models.UserEvent

import com.softwaremill.quicklens._
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.BoolAlgebra.all

object processInteractionSuite extends DefaultRunnableSpec {

  import GameLogic.initializeGame
  import GameLogic.processInteraction

  import Generators.genHand
  import Generators.genHandN
  import Generators.genPlayerTag
  import Generators.genHandAndIndex
  import Generators.genHandAndInvalidIndex

  private val endTurnSuite =
    suite("ending turn")(
      testM("should increase turn and current mana if current player was second")(
        for {
          game     <- initializeGame
          modGame   = game.copy(currentPlayerTag = PlayerTag.Second)
          procGame <- processInteraction(modGame, UserEvent.EndTurn)
        } yield all(
          assert(procGame.turn)(equalTo(2)),
          assert(procGame.currentMana)(equalTo(2))
        )
      ),
      testM("should switch the player and set the mana equal to turn if turn less than 10")(
        checkM(genPlayerTag, Gen.int(1, 9))((currentPlayer, turn) =>
          for {
            game   <- initializeGame
            modGame = game.copy(turn = turn, currentPlayerTag = currentPlayer)

            procGame <- processInteraction(modGame, UserEvent.EndTurn)
          } yield all(
            assert(procGame.currentPlayerTag)(equalTo(currentPlayer.other)),
            assert(procGame.currentMana)(equalTo(procGame.turn))
          )
        )
      ),
      testM("should set the mana to 10 if turn is more than 10")(
        checkM(genPlayerTag, Gen.int(10, 100))((currentPlayerTag, turn) =>
          for {
            game     <- initializeGame
            modGame   = game.copy(turn = turn, currentPlayerTag = currentPlayerTag)
            procGame <- processInteraction(modGame, UserEvent.EndTurn)
          } yield all(
            assert(procGame.currentPlayerTag)(equalTo(currentPlayerTag.other)),
            assert(procGame.currentMana)(equalTo(10))
          )
        )
      ),
      testM("should draw the top card from the next player's deck into his hand, if his hand has less than 5 cards")(
        checkM(genPlayerTag, genHand.filter(_.size < 5))((thePlayerTag, hand) =>
          for {
            initGame <- initializeGame
            preGame   = initGame
                          .modify(_.currentPlayerTag).setTo(thePlayerTag.other)
                          .modify(_.players.at(thePlayerTag).hand).setTo(hand)
            prePlayer = preGame.players(thePlayerTag)

            postGame  <- processInteraction(preGame, UserEvent.EndTurn)
            postPlayer = postGame.players(thePlayerTag)
          } yield all(
            assert(postPlayer.hand.cards)(equalTo(prePlayer.deck.cards.head +: prePlayer.hand.cards)),
            assert(postPlayer.deck.cards)(equalTo(prePlayer.deck.cards.tail))
          )
        )
      ),
      testM("should discard the top card from next player's the deck, if his hand has 5 cards")(
        checkM(genPlayerTag, genHandN(5))((thePlayerTag, fullHand) =>
          for {
            initGame <- initializeGame
            preGame   = initGame
                          .modify(_.currentPlayerTag).setTo(thePlayerTag.other)
                          .modify(_.players.at(thePlayerTag).hand).setTo(fullHand)
            prePlayer = preGame.players(thePlayerTag)

            postGame  <- processInteraction(preGame, UserEvent.EndTurn)
            postPlayer = postGame.players(thePlayerTag)
          } yield all(
            assert(postPlayer.hand.cards)(equalTo(prePlayer.hand.cards)),
            assert(postPlayer.deck.cards)(equalTo(prePlayer.deck.cards.tail))
          )
        )
      ),
      testM("should should bleed the next player and keep his hand same, if his deck has no cards left")(
        checkM(genPlayerTag, genHandN(5))((thePlayerTag, fullHand) =>
          for {
            initGame <- initializeGame
            preGame   = initGame
                          .modify(_.currentPlayerTag).setTo(thePlayerTag.other)
                          .modify(_.players.at(thePlayerTag).deck).setTo(Deck.empty)
            prePlayer = preGame.players(thePlayerTag)

            postGame  <- processInteraction(preGame, UserEvent.EndTurn)
            postPlayer = postGame.players(thePlayerTag)
          } yield all(
            assert(postPlayer.hand.cards)(equalTo(prePlayer.hand.cards)),
            assert(postPlayer.deck.cards)(equalTo(prePlayer.deck.cards)),
            assert(postPlayer.currentHealth)(equalTo(prePlayer.currentHealth - 1))
          )
        )
      )
    )

  private val playCardSuite = suite("playing card")(
    testM("should deal damage equal to its mana cost to other player and discard it from current user's hand")(
      checkM(genPlayerTag, genHandAndIndex) {
        case (thePlayerTag, (hand, handIndex)) =>
          for {
            initGame            <- initializeGame
            preGame              = initGame
                                     .modify(_.currentMana).setTo(10)
                                     .modify(_.currentPlayerTag).setTo(thePlayerTag)
                                     .modify(_.players.at(thePlayerTag).hand).setTo(hand)
            preGameCurrentPlayer = preGame.players(thePlayerTag)
            preGameOtherPlayer   = preGame.players(thePlayerTag.other)

            playedCard           <- ZIO.fromEither(hand.cardAt(handIndex))
            postGame             <- processInteraction(preGame, UserEvent.PlayCard(handIndex))
            postGameCurrentPlayer = postGame.players(thePlayerTag)
            postGameOtherPlayer   = postGame.players(thePlayerTag.other)

          } yield all(
            assert(postGameOtherPlayer.currentHealth)(equalTo(preGameOtherPlayer.currentHealth - playedCard.manaCost)),
            assert(postGameCurrentPlayer.hand.countOf(playedCard))(
              equalTo(preGameCurrentPlayer.hand.countOf(playedCard) - 1)
            )
          )
      }
    ),
    testM("should reduce the current mana by mana cost of the played card")(
      checkM(genPlayerTag, genHandAndIndex) {
        case (thePlayerTag, (hand, handIndex)) =>
          for {
            initGame   <- initializeGame
            preGame     = initGame
                            .modify(_.currentMana).setTo(10)
                            .modify(_.currentPlayerTag).setTo(thePlayerTag)
                            .modify(_.players.at(thePlayerTag).hand).setTo(hand)

            playedCard <- ZIO.fromEither(hand.cardAt(handIndex))
            postGame   <- processInteraction(preGame, UserEvent.PlayCard(handIndex))
          } yield all(
            assert(postGame.currentMana)(equalTo(preGame.currentMana - playedCard.manaCost))
          )
      }
    ),
    testM("should not be possible if attempted card has more mana cost than the current mana")(
      checkM(genPlayerTag, genHandAndIndex.filter({ case (hand, index) => hand.cards(index).manaCost > 0 })) {
        case (thePlayerTag, (hand, handIndex)) =>
          val playedCard = hand.cards(handIndex)
          for {
            initGame        <- initializeGame
            preGame          = initGame
                                 .modify(_.currentMana).setTo(playedCard.manaCost - 1)
                                 .modify(_.currentPlayerTag).setTo(thePlayerTag)
                                 .modify(_.players.at(thePlayerTag).hand).setTo(hand)

            postGameOrError <- processInteraction(preGame, UserEvent.PlayCard(handIndex)).either
          } yield assert(postGameOrError)(equalTo(Left(NotEnoughMana(
            card = playedCard,
            currentMana = playedCard.manaCost - 1
          ))))
      }
    ),
    testM("should not be possible if no cards left in hand or card index is out of hand's bounds")(
      checkM(genPlayerTag, genHandAndInvalidIndex) {
        case (thePlayerTag, (hand, invalidHandIndex)) =>
          for {
            initGame        <- initializeGame
            preGame          = initGame
                                 .modify(_.currentPlayerTag).setTo(thePlayerTag)
                                 .modify(_.players.at(thePlayerTag).hand).setTo(hand)

            postGameOrError <- processInteraction(preGame, UserEvent.PlayCard(invalidHandIndex)).either
          } yield assert(postGameOrError)(equalTo(Left(InvalidHandIndex(hand, invalidHandIndex))))
      }
    )
  )

  override def spec =
    suite("gameLogic.processInteraction")(
      endTurnSuite,
      playCardSuite
    ).provideCustomLayer(GameLogic.live)

}
