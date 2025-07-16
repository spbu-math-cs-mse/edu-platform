package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class AskStudentFirstNameState(
  override val context: User,
  private val token: String?,
  private val from: String? = null,
) : BotState<String, Unit, StudentApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: StudentApi,
  ): Result<String, FrontendError> =
    runCatching {
        bot.sendSticker(context, Dialogues.greetingSticker)
        bot.send(context, Dialogues.greetings)
        bot.send(context, Dialogues.askFirstName)
        val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
        firstName
      }
      .toTelegramError()

  override suspend fun computeNewState(
    service: StudentApi,
    input: String,
  ): Result<Pair<State, Unit>, FrontendError> =
    (AskStudentLastNameState(context, input, token, from) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
