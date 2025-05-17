package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
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

class DeveloperStartState(override val context: User) : BotState<TeacherId?, String, TeacherApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherApi): TeacherId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    bot.send(context, Dialogues.devAskForId)
    return bot
      .waitTextMessageWithUser(context.id)
      .first()
      .content
      .text
      .toLongOrNull()
      ?.toTeacherId()
  }

  override fun computeNewState(service: TeacherApi, input: TeacherId?): Pair<State, String> =
    binding {
        val teacherId = input.toResultOr { Dialogues.devIdIsNotLong }.bind()
        service.loginById(teacherId).mapError { Dialogues.devIdNotFound }.bind()
        Pair(MenuState(context, teacherId), Dialogues.greetings())
      }
      .getOrElse { Pair(DeveloperStartState(context), it) }

  override suspend fun sendResponse(bot: BehaviourContext, service: TeacherApi, response: String) {
    bot.send(context, response)
  }
}
