package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.util.BotState
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

class DeveloperStartState(override val context: User) :
  BotState<StudentId?, String, StudentStorage> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentStorage): StudentId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    bot.send(context, Dialogues.devAskForId())
    val studentIdFromText =
      bot.waitTextMessageWithUser(context.id).first().content.text.toLongOrNull()?.let {
        StudentId(it)
      }
    return studentIdFromText
  }

  override fun computeNewState(service: StudentStorage, input: StudentId?): Pair<State, String> =
    binding {
        val studentId = input.toResultOr { Dialogues.devIdIsNotLong() }.bind()
        service.resolveStudent(studentId).mapError { Dialogues.devIdNotFound() }.bind()
        Pair(MenuState(context, studentId), Dialogues.greetings())
      }
      .getOrElse { Pair(DeveloperStartState(context), it) }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentStorage,
    response: String,
  ) {
    bot.send(context, response)
  }
}
