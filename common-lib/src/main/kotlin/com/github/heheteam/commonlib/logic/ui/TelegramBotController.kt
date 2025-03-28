package com.github.heheteam.commonlib.logic.ui

import dev.inmo.tgbotapi.bot.TelegramBot

interface TelegramBotController {
  fun setTelegramBot(telegramBot: TelegramBot)
}
