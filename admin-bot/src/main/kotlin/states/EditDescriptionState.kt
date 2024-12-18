package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.utils.newLine
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnEditDescriptionState() {
    strictlyOn<EditDescriptionState> { state ->
        send(
            state.context,
        ) {
            +"Введите новое описание курса ${state.courseName}. Текущее описание:" + newLine + newLine
            +state.course.name
        }
        val message = waitTextMessageWithUser(state.context.id).first()
        val answer = message.content.text
        when {
            answer == "/stop" -> MenuState(state.context)

            else -> {
//        state.course.name = answer TODO: implement this feature
                send(
                    state.context,
                    "Описание курса ${state.courseName} успешно обновлено",
                )

                MenuState(state.context)
            }
        }
    }
}
