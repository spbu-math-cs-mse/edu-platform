package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.Course
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

      core.courseExists(answer) -> {
        send(
          state.context,
        ) {
          +"Курс с таким названием уже существует"
        }
        CreateCourseState(state.context)
      }

      else -> {
        core.addCourse(answer, Course(0L, mutableListOf(), mutableListOf(), "", core))

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
