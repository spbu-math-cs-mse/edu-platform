package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.dateFormatter
import com.github.heheteam.adminbot.timeFormatter
import com.github.heheteam.adminbot.toRussian
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.ScheduledMessage
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.newLine
import dev.inmo.tgbotapi.utils.row
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.first

class AddScheduledMessageState(override val context: User, val course: Course) : State

fun DefaultBehaviourContextWithFSM<State>.strictlyOnAddScheduledMessageState(core: AdminApi) {
  strictlyOn<AddScheduledMessageState> { state ->
    send(state.context) { +"Введите сообщение" }
    val message = waitTextMessageWithUser(state.context.id).first()
    val text = message.content.text

    if (text == "/stop") {
      return@strictlyOn MenuState(state.context)
    }

    val date = queryDateFromUser(state) ?: return@strictlyOn MenuState(state.context)
    val time = queryTimeFromUser(state) ?: return@strictlyOn MenuState(state.context)

    send(state.context) {
      +"Сообщение успешно добавлено:" +
        newLine +
        text +
        newLine +
        "Время отправки: " +
        time.format(timeFormatter) +
        " " +
        date.format(dateFormatter) +
        newLine +
        "Курс: " +
        state.course.name
    }
    core.addMessage(ScheduledMessage(state.course, LocalDateTime.of(date, time), text))
    MenuState(state.context)
  }
}

private suspend fun BehaviourContext.queryDateFromUser(
  state: AddScheduledMessageState
): LocalDate? {
  send(
    state.context,
    "Выберите дату или введите её в формате дд.мм.гггг",
    replyMarkup = keyboardWithDates(),
  )

  val callback = waitDataCallbackQueryWithUser(state.context.id).first()
  val data = callback.data
  answerCallbackQuery(callback)
  when (data) {
    "enter date" -> {
      while (true) {
        val message = waitTextMessageWithUser(state.context.id).first().content.text
        if (message == "/stop") {
          return null
        }
        try {
          val date = LocalDate.parse(message, dateFormatter)
          return date
        } catch (_: DateTimeParseException) {
          send(
            state.context,
            "Неправильный формат, введите дату в формате дд.мм.гггг" +
              " или /stop, чтобы отменить операцию",
          )
        }
      }
    }

    "cancel" -> return null
    else -> return LocalDate.parse(data, dateFormatter)
  }
}

private suspend fun BehaviourContext.queryTimeFromUser(
  state: AddScheduledMessageState
): LocalTime? {
  send(state.context, "Введите время в формате чч:мм")
  while (true) {
    val message = waitTextMessageWithUser(state.context.id).first().content.text
    if (message == "/stop") {
      return null
    }
    try {
      val time = LocalTime.parse(message, timeFormatter)
      return time
    } catch (_: DateTimeParseException) {
      send(
        state.context,
        "Неправильный формат, введите время в формате чч::мм" +
          " или /stop, чтобы отменить операцию",
      )
    }
  }
}

private fun keyboardWithDates(): InlineKeyboardMarkup {
  val today = LocalDate.now()
  val dates =
    listOf(
      today,
      today.plusDays(1),
      today.plusDays(2),
      today.plusDays(3),
      today.plusDays(4),
      today.plusDays(5),
      today.plusDays(6),
    )
  return InlineKeyboardMarkup(
    keyboard =
      matrix {
        row {
          dataButton(dates[0].format(dateFormatter) + " (сегодня)", dates[0].format(dateFormatter))
        }
        row {
          dataButton(dates[1].format(dateFormatter) + " (завтра)", dates[1].format(dateFormatter))
        }
        row {
          dataButton(
            dates[2].format(dateFormatter) + " (" + toRussian(dates[2].dayOfWeek) + ")",
            dates[2].format(dateFormatter),
          )
        }
        row {
          dataButton(
            dates[3].format(dateFormatter) + " (" + toRussian(dates[3].dayOfWeek) + ")",
            dates[3].format(dateFormatter),
          )
        }
        row {
          dataButton(
            dates[4].format(dateFormatter) + " (" + toRussian(dates[4].dayOfWeek) + ")",
            dates[4].format(dateFormatter),
          )
        }
        row {
          dataButton(
            dates[5].format(dateFormatter) + " (" + toRussian(dates[5].dayOfWeek) + ")",
            dates[5].format(dateFormatter),
          )
        }
        row {
          dataButton(
            dates[6].format(dateFormatter) + " (" + toRussian(dates[6].dayOfWeek) + ")",
            dates[6].format(dateFormatter),
          )
        }
        row { dataButton("Ввести с клавиатуры", "enter date") }
        row { dataButton("Отмена", "cancel") }
      }
  )
}
