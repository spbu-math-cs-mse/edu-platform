package com.github.heheteam.adminbot.states

import Student
import com.github.heheteam.adminbot.mockStudents
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveStudentState() {
    strictlyOn<RemoveStudentState> { state ->
        val message = waitTextMessage().first()
        val id = message.content.text
        when {
            id == "/stop" -> StartState(state.context)

            !mockStudents.containsKey(id) -> {
                send(
                    state.context,
                    "Ученика с идентификатором $id не существует. Попробуйте ещё раз или отправьте /stop, чтобы отменить операцию",
                )
                RemoveStudentState(state.context, state.course, state.courseName)
            }

            else -> {
                if (state.course.students.remove(Student(id))) {
                    send(
                        state.context,
                        "Ученик $id успешно удалён с курса ${state.courseName}",
                    )
                } else {
                    send(
                        state.context,
                        "Ученика $id нет на курсе ${state.courseName}",
                    )
                }
                StartState(state.context)
            }
        }
    }
}