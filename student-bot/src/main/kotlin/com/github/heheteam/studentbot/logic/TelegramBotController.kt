package com.github.heheteam.studentbot.logic

import dev.inmo.tgbotapi.bot.TelegramBot

interface TelegramBotController {
  fun setTelegramBot(telegramBot: TelegramBot)
}
