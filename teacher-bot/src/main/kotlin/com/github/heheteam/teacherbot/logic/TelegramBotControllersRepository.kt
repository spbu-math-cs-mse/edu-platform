package com.github.heheteam.teacherbot.logic

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info

class TelegramBotControllersRepository {
  private val botControllers = mutableListOf<TelegramBotController>()

  fun add(controller: TelegramBotController) {
    botControllers.add(controller)
    KSLog.info("added controller $controller")
  }

  fun get(): List<TelegramBotController> = botControllers
}
