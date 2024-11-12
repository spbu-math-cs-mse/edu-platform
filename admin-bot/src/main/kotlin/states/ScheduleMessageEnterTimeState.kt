package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.dateFormatter
import com.github.heheteam.adminbot.mockScheduledMessages
import com.github.heheteam.adminbot.timeFormatter
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.utils.newLine
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnScheduleMessageEnterTimeState() {
  strictlyOn<ScheduleMessageEnterTimeState> { state ->
    val message = waitTextMessage().first().content.text
    if (message == "/stop") {
      return@strictlyOn StartState(state.context)
    }
    try {
      val time = LocalTime.parse(message, timeFormatter)
      send(state.context) {
        +"Сообщение успешно добавлено:" + newLine + state.text + newLine +
          "Время отправки: " + time.format(timeFormatter) + " " +
          state.date.format(dateFormatter) + newLine +
          "Курс: " + state.courseName
      }
      mockScheduledMessages[state.course] = LocalDateTime.of(state.date, time) to state.text
      StartState(state.context)
    } catch (e: DateTimeParseException) {
      send(
        state.context,
        "Неправильный формат, введите время в формате чч::мм" +
          " или /stop, чтобы отменить операцию",
      )
      return@strictlyOn ScheduleMessageEnterTimeState(
        state.context,
        state.course,
        state.courseName,
        state.text,
        state.date,
      )
    }
  }
}
