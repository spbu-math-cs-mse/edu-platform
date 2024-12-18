package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCreateCourseState(core: AdminCore) {
    strictlyOn<CreateCourseState> { state ->
        send(
            state.context,
            "Введите название курса, который хотите создать, или отправьте /stop, чтобы отменить операцию",
        )

        val message = waitTextMessageWithUser(state.context.id).first()
        val answer = message.content.text

        when {
            answer == "/stop" -> MenuState(state.context)

            core.courseExists(answer) -> {
                send(state.context, "Курс с таким названием уже существует")
                MenuState(state.context)
            }

            else -> {
                core.addCourse(answer)
                send(state.context, "Курс $answer успешно создан")
                MenuState(state.context)
            }
        }
    }
}
