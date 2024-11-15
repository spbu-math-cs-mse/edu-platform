package com.github.heheteam.adminbot.states

import Course
import com.github.heheteam.adminbot.AdminCore
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCreateCourseState(core: AdminCore) {
  strictlyOn<CreateCourseState> { state ->
    val message = waitTextMessage().first()
    val answer = message.content.text

    when {
      answer == "/stop" ->
        StartState(state.context)

      core.coursesTable.containsKey(answer) -> {
        send(
          state.context,
        ) {
          +"Курс с таким названием уже существует"
        }
        CreateCourseState(state.context)
      }

      else -> {
        core.coursesTable[answer] = Course(mutableListOf(), mutableListOf(), "", core)

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
