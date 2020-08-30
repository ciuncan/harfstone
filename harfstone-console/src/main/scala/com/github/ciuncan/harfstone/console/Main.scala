package com.github.ciuncan.harfstone.console

import com.github.ciuncan.harfstone.core.services.gameLogic.GameLogic

import com.github.ciuncan.harfstone.console.consoleEngine.ConsoleEngine
import com.github.ciuncan.harfstone.console.userInterface.ExitGameRequest
import com.github.ciuncan.harfstone.console.userInterface.UserInterface

import zio._

/**
  * Entry point of harfstone console application.
  * @see https://www.mindissoftware.com/post/2020/03/zio3-module/
  */
object Main extends zio.App {

  def fullLayer: URLayer[ZEnv, ConsoleEngine] =
    (UserInterface.live ++ GameLogic.live) >>> ConsoleEngine.live

  def program: ZIO[ZEnv, ExitGameRequest, Unit] =
    ConsoleEngine.startGame.provideLayer(fullLayer)

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.exitCode

}
