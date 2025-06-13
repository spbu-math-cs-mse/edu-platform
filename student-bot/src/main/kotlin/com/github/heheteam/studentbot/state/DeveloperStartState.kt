package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class DeveloperStartState(override val context: User) : BotState<StudentId?, String, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi): StudentId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    bot.send(context, Dialogues.devAskForId)
    return bot
      .waitTextMessageWithUser(context.id)
      .first()
      .content
      .text
      .toLongOrNull()
      ?.toStudentId()
  }

  override suspend fun computeNewState(service: StudentApi, input: StudentId?): Pair<State, String> =
    binding {
        val studentId = input.toResultOr { Dialogues.devIdIsNotLong }.bind()
        service.loginById(studentId).mapError { Dialogues.devIdNotFound }.bind()
        Pair(MenuState(context, studentId), Dialogues.greetings)
      }
      .getOrElse { Pair(DeveloperStartState(context), it) }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: String) {
    bot.send(context, response)
  }
}
