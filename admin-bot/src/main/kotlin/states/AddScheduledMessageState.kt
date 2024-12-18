package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.*
import com.github.heheteam.commonlib.api.ScheduledMessage
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.newLine
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddScheduledMessageState(core: AdminCore) {
    strictlyOn<AddScheduledMessageState> { state ->
        send(
            state.context,
        ) {
            +"Введите сообщение"
        }
        val message = waitTextMessageWithUser(state.context.id).first()
        val text = message.content.text

        if (text == "/stop") {
            return@strictlyOn MenuState(state.context)
        }

        val date = queryDateFromUser(state) ?: return@strictlyOn MenuState(state.context)
        val time = queryTimeFromUser(state) ?: return@strictlyOn MenuState(state.context)

        send(state.context) {
            +"Сообщение успешно добавлено:" + newLine + text + newLine +
                "Время отправки: " + time.format(timeFormatter) + " " +
                date.format(dateFormatter) + newLine +
                "Курс: " + state.courseName
        }
        core.addMessage(ScheduledMessage(state.course, LocalDateTime.of(date, time), text))
        MenuState(state.context)
    }
}

private suspend fun BehaviourContext.queryDateFromUser(state: AddScheduledMessageState): LocalDate? {
    send(
        state.context,
        "Выберите дату или введите её в формате дд.мм.гггг",
        replyMarkup =
        keyboardWithDates(),
    )

    val callback = waitDataCallbackQueryWithUser(state.context.id).first()
    val data = callback.data
    answerCallbackQuery(callback)
    when (data) {
        "enter date" ->
            {
                while (true) {
                    val message = waitTextMessageWithUser(state.context.id).first().content.text
                    if (message == "/stop") {
                        return null
                    }
                    try {
                        val date = LocalDate.parse(message, dateFormatter)
                        return date
                    } catch (e: DateTimeParseException) {
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

private suspend fun BehaviourContext.queryTimeFromUser(state: AddScheduledMessageState): LocalTime? {
    send(
        state.context,
        "Введите время в формате чч:мм",
    )
    while (true) {
        val message = waitTextMessageWithUser(state.context.id).first().content.text
        if (message == "/stop") {
            return null
        }
        try {
            val time = LocalTime.parse(message, timeFormatter)
            return time
        } catch (e: DateTimeParseException) {
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
    val dates = generateDatesList(today)

    return InlineKeyboardMarkup(
        keyboard = matrix {
            addTodayTomorrowRows(dates)
            addWeekdayRows(dates.drop(2))
            addControlRows()
        },
    )
}

private fun MatrixBuilder<InlineKeyboardButton>.addTodayTomorrowRows(dates: List<LocalDate>) {
    row {
        dataButton(
            "${dates[0].format(dateFormatter)} (сегодня)",
            dates[0].format(dateFormatter),
        )
    }
    row {
        dataButton(
            "${dates[1].format(dateFormatter)} (завтра)",
            dates[1].format(dateFormatter),
        )
    }
}

private fun MatrixBuilder<InlineKeyboardButton>.addWeekdayRows(dates: List<LocalDate>) {
    dates.forEach { date ->
        row {
            dataButton(
                "${date.format(dateFormatter)} (${toRussian(date.dayOfWeek)})",
                date.format(dateFormatter),
            )
        }
    }
}

private fun MatrixBuilder<InlineKeyboardButton>.addControlRows() {
    row {
        dataButton("Ввести с клавиатуры", "enter date")
    }
    row {
        dataButton("Отмена", "cancel")
    }
}

private fun generateDatesList(startDate: LocalDate): List<LocalDate> =
    (0..6).map { startDate.plusDays(it.toLong()) }
