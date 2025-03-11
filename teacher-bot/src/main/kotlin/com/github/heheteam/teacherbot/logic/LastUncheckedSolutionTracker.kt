package com.github.heheteam.teacherbot.logic

import dev.inmo.tgbotapi.bot.TelegramBot

interface LastUncheckedSolutionTracker {
  fun updateMenuReplyInPersonalChat()

  fun setTelegramBot(telegramBot: TelegramBot)
}
