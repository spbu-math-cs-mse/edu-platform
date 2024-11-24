package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.textsources.botCommand
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState(core: AdminCore) {
  strictlyOn<MenuState> { state ->
    val callback = waitDataCallbackQuery().first()
    val data = callback.data
    answerCallbackQuery(callback)
    when (data) {
      "create course" -> {
        send(
          state.context,
        ) {
          +"Введите название курса, который хотите создать, или отправьте " + botCommand("stop") + ", чтобы отменить операцию"
        }
        CreateCourseState(state.context)
      }
      "edit course" -> {
        bot.send(
          state.context,
          "Выберите курс, который хотите изменить:",
          replyMarkup =
          replyKeyboard {
            for ((name, _) in core.getCourses()) {
              row {
                simpleButton(
                  text = name,
                )
              }
            }
          },
        )

        val message = waitTextMessage().first()
        val answer = message.content.text

        val msg =
          bot.send(
            state.context,
            "...",
            replyMarkup = ReplyKeyboardRemove(),
          )
        bot.delete(msg)

        if (answer == "/stop") {
          StartState(state.context)
        }

        EditCourseState(state.context, core.getCourse(answer)!!, answer)
      }
      else -> MenuState(state.context)
    }
  }
}
