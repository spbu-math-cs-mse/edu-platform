package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.api.ScheduledMessage
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import java.time.LocalDateTime

class InMemoryScheduledMessagesDistributor(
  private val messages: MutableMap<ScheduledMessage, Boolean> = mutableMapOf(),
) : ScheduledMessagesDistributor {
  override fun addMessage(message: ScheduledMessage) {
    messages[message] = false
  }

  override fun getUnsentMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> {
    val res = mutableListOf<ScheduledMessage>()
    for ((message, isSent) in messages) {
      if (!isSent && date.isAfter(message.date)) {
        res.addLast(message)
      }
    }
    return res
  }

  override fun markMessagesUpToDateAsSent(date: LocalDateTime) {
    for (message in messages) {
      if (!message.value && date.isAfter(message.key.date)) {
        messages[message.key] = true
      }
    }
  }
}
