package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StartState(override val context: User, private val token: String?) :
  BotState<StudentId?, String?, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi): StudentId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
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

  override suspend fun computeNewState(
    service: StudentApi,
    input: StudentId?,
  ): Pair<State, String?> =
    if (input != null) {
      MenuState(context, input) to Dialogues.greetings
    } else {
      AskFirstNameState(context, token) to Dialogues.greetings
    }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: String?) {
    if (response != null) {
      bot.send(context, response)
    }
  }
}
