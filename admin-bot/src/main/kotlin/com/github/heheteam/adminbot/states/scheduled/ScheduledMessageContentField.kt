package com.github.heheteam.adminbot.states.scheduled

import com.github.heheteam.commonlib.TelegramMessageContent

data class ScheduledMessageContentField(
  val shortDescription: String,
  val content: TelegramMessageContent,
)
