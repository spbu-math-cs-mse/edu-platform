package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.dateFormatter
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnScheduleMessageEnterDateState() {
  strictlyOn<ScheduleMessageEnterDateState> { state ->
    val message = waitTextMessage().first().content.text
    if (message == "/stop") {
      return@strictlyOn StartState(state.context)
    }
    try {
      val date = LocalDate.parse(message, dateFormatter)
      send(state.context, "Введите время в формате чч:мм")
      ScheduleMessageEnterTimeState(
        state.context,
        state.course,
        state.courseName,
        state.text,
        date,
      )
    } catch (e: DateTimeParseException) {
      send(
        state.context,
        "Неправильный формат, введите дату в формате дд.мм.гггг" +
          " или /stop, чтобы отменить операцию",
      )
      return@strictlyOn ScheduleMessageEnterDateState(
        state.context,
        state.course,
        state.courseName,
        state.text,
      )
    }
  }
}
