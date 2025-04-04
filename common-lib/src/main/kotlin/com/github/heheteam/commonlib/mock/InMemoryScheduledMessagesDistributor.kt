package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.interfaces.ScheduledMessage
import com.github.heheteam.commonlib.interfaces.ScheduledMessagesDistributor
import java.time.LocalDateTime

class InMemoryScheduledMessagesDistributor(
  private val messages: MutableMap<ScheduledMessage, Boolean> = mutableMapOf()
) : ScheduledMessagesDistributor {
  override fun addMessage(message: ScheduledMessage) {
    messages[message] = false
  }

  override fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> {
    val res = mutableListOf<ScheduledMessage>()
    for (message in messages) {
      if (!message.value && date.isAfter(message.key.date)) {
        res.addLast(message.key)
      }
    }
    return res.toList()
  }

  override fun markMessagesUpToDateAsSent(date: LocalDateTime) {
    for (message in messages) {
      if (!message.value && date.isAfter(message.key.date)) {
        messages[message.key] = true
      }
    }
  }
}
