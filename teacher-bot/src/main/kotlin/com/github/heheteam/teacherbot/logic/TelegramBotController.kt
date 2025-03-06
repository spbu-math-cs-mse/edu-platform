package com.github.heheteam.teacherbot.logic

import dev.inmo.tgbotapi.bot.TelegramBot

interface TelegramBotController {
  fun setTelegramBot(telegramBot: TelegramBot)
}
