package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.dateFormatter
import com.github.heheteam.adminbot.toRussian
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import java.time.LocalDate

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddScheduledMessageState() {
    strictlyOn<AddScheduledMessageState> { state ->
        val message = waitTextMessage().first()
        val text = message.content.text
        when {
            text == "/stop" -> StartState(state.context)

            else -> {
                val today = LocalDate.now()
                val dates = listOf(today, today.plusDays(1),
                    today.plusDays(2), today.plusDays(3), today.plusDays(4),
                    today.plusDays(5), today.plusDays(6))

                send(state.context,
                    "Выберите дату или введите её в формате дд.мм.гг",
                    replyMarkup = InlineKeyboardMarkup(
                        keyboard = matrix {
                            row {
                                dataButton(dates[0].format(dateFormatter) + " (сегодня)",
                                    dates[0].format(dateFormatter))
                            }
                            row {
                                dataButton(dates[1].format(dateFormatter) + " (завтра)",
                                    dates[1].format(dateFormatter))
                            }
                            row {
                                dataButton(dates[2].format(dateFormatter) +
                                        " (" + toRussian(dates[2].dayOfWeek) + ")",
                                    dates[2].format(dateFormatter))
                            }
                            row {
                                dataButton(dates[3].format(dateFormatter) +
                                        " (" + toRussian(dates[3].dayOfWeek) + ")",
                                    dates[3].format(dateFormatter))
                            }
                            row {
                                dataButton(dates[4].format(dateFormatter) +
                                        " (" + toRussian(dates[4].dayOfWeek) + ")",
                                    dates[4].format(dateFormatter))
                            }
                            row {
                                dataButton(dates[5].format(dateFormatter) +
                                        " (" + toRussian(dates[5].dayOfWeek) + ")",
                                    dates[5].format(dateFormatter))
                            }
                            row {
                                dataButton(dates[6].format(dateFormatter) +
                                        " (" + toRussian(dates[6].dayOfWeek) + ")",
                                    dates[6].format(dateFormatter))
                            }
                            row {
                                dataButton("Ввести с клавиатуры", "enter date")
                            }
                            row {
                                dataButton("Отмена", "cancel")
                            }
                        }
                    )
                )
                ScheduleMessageSelectDateState(state.context, state.course, state.courseName, text)
            }
        }
    }
}
