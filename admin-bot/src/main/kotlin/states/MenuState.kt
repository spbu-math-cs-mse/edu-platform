package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.mockCourses
import dev.inmo.tgbotapi.extensions.api.answers.answerCallbackQuery
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.replyKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.simpleButton
import dev.inmo.tgbotapi.types.message.textsources.botCommand
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
    strictlyOn<MenuState> { state ->
        val callback = waitDataCallbackQuery().first()
        val data = callback.data
        answerCallbackQuery(callback)
        when {
            data == "create course" -> {
                send(
                    state.context,
                ) {
                    +"Введите название курса, который хотите создать, или отправьте " + botCommand("stop") + ", чтобы отменить операцию"
                }
                CreateCourseState(state.context)
            }

            data == "edit course" -> {
                bot.send(
                    state.context,
                    "Выберите курс, который хотите изменить:",
                    replyMarkup =
                    replyKeyboard {
                        for ((name, _) in mockCourses) {
                            row {
                                simpleButton(
                                    text = name,
                                )
                            }
                        }
                    },
                )
                PickACourseState(state.context)
            }

            else -> MenuState(state.context)
        }
    }
}