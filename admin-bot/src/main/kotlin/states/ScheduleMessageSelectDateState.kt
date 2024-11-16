package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.dateFormatter
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnScheduleMessageSelectDateState() {
  strictlyOn<ScheduleMessageSelectDateState> { state ->
    val callback = waitDataCallbackQuery().first()
    val data = callback.data
    answerCallbackQuery(callback)
    when {
      data == "enter date" -> ScheduleMessageEnterDateState(
        state.context,
        state.course,
        state.courseName,
        state.text,
      )

      data == "cancel" -> StartState(state.context)

      else -> {
        send(
          state.context,
          "Введите время в формате чч:мм",
        )
        ScheduleMessageEnterTimeState(
          state.context,
          state.course,
          state.courseName,
          state.text,
          LocalDate.parse(data, dateFormatter),
        )
      }
    }
  }
}
