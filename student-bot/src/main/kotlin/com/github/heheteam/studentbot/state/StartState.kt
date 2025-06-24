package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StartState(override val context: User, private val token: String?) :
  BotState<StudentId?, Unit, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi): StudentId? {
    val id = service.loginByTgId(context.id).get()?.id
    if (id != null) {
      if (token != null) {
        service
          .registerForCourseWithToken(token, id)
          .mapBoth(
            success = { course ->
              bot.send(context, Dialogues.successfullyRegisteredForCourse(course, token))
            },
            failure = { error -> bot.send(context, Dialogues.failedToRegisterForCourse(error)) },
          )
      }
    }
    return id
  }

  override suspend fun computeNewState(service: StudentApi, input: StudentId?): Pair<State, Unit> =
    if (input != null) {
      MenuState(context, input) to Unit
    } else {
      AskFirstNameState(context, token) to Unit
    }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) =
    Unit
}
