package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.mockCourses
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnPickACourseState() {
  strictlyOn<PickACourseState> { state ->
    val message = waitTextMessage().first()
    val answer = message.content.text

    val msg = bot.send(
      state.context,
      "...",
      replyMarkup = ReplyKeyboardRemove(),
    )
    bot.delete(msg)

    when {
      answer == "/stop" -> {
        StartState(state.context)
      }

      else -> {
        bot.send(
          state.context,
          "Изменить курс $answer:",
          replyMarkup =
          inlineKeyboard {
            row {
              dataButton("Добавить ученика", "add a student")
            }
            row {
              dataButton("Убрать ученика", "remove a student")
            }
            row {
              dataButton("Добавить преподавателя", "add a teacher")
            }
            row {
              dataButton("Убрать преподавателя", "remove a teacher")
            }
            row {
              dataButton("Изменить описание", "edit description")
            }
            row {
              dataButton("Добавить отложенное сообщение", "add scheduled message")
            }
            row {
              dataButton("Назад", "cancel")
            }
          },

        )
        mockCourses[answer]?.let { EditCourseState(state.context, it, answer) }
      }
    }
  }
}
