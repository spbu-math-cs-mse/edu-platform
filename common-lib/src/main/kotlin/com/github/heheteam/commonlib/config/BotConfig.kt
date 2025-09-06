package com.github.heheteam.commonlib.config

data class BotConfig(
  val studentBotToken: String,
  val teacherBotToken: String,
  val adminBotToken: String,
  val adminIds: List<Long>,
  val studentBotUsername: String,
)
