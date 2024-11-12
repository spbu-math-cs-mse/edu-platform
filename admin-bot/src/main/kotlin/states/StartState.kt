package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.mockAdmins
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState() {
    strictlyOn<StartState> { state ->
        if (state.context.username == null) {
            return@strictlyOn null
        }
        val username = state.context.username!!.username
        if (mockAdmins.containsValue(username)) {
            bot.send(
                state.context,
                "Главное меню:",
                replyMarkup =
                InlineKeyboardMarkup(
                    keyboard =
                    matrix {
                        row {
                            dataButton("Создать курс", "create course")
                        }
                        row {
                            dataButton("Изменить курс", "edit course")
                        }
                    },
                ),
            )
            MenuState(state.context)
        } else {
            send(
                state.context,

                "У вас нет прав администратора",
                replyMarkup = InlineKeyboardMarkup(
                    keyboard = matrix {
                        row {
                            dataButton("Проверить ещё раз", "update")
                        }
                    },
                ),
            )
            NotAdminState(state.context)
        }
    }
}