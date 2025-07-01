package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.teacherbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StartState(override val context: User) : BotState<TeacherId?, String?, TeacherApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherApi,
  ): Result<TeacherId?, FrontendError> =
    runCatching {
        bot.sendSticker(context, Dialogues.greetingSticker)
        service.loginByTgId(context.id).get()?.id
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: TeacherApi,
    input: TeacherId?,
  ): Result<Pair<State, String?>, FrontendError> =
    if (input != null) {
        MenuState(context, input) to Dialogues.greetings
      } else {
        AskFirstNameState(context) to Dialogues.greetings
      }
      .ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherApi,
    response: String?,
  ): Result<Unit, FrontendError> =
    runCatching {
        if (response != null) {
          bot.send(context, response)
        }
      }
      .toTelegramError()
}
