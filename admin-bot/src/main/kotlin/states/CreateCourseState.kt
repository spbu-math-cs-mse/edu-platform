package com.github.heheteam.adminbot.states

import Course
import com.github.heheteam.adminbot.mockCourses
import com.github.heheteam.adminbot.mockGradeTable
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCreateCourseState() {
    strictlyOn<CreateCourseState> { state ->
        val message = waitTextMessage().first()
        val answer = message.content.text

        when {
            answer == "/stop" ->
                StartState(state.context)

            mockCourses.containsKey(answer) -> {
                send(
                    state.context,
                ) {
                    +"Курс с таким названием уже существует"
                }
                CreateCourseState(state.context)
            }

            else -> {
                mockCourses.put(answer, Course(mutableListOf(), mutableListOf(), "", mockGradeTable))

                send(
                    state.context,
                ) {
                    +"Курс $answer успешно создан"
                }
                StartState(state.context)
            }
        }
    }
}