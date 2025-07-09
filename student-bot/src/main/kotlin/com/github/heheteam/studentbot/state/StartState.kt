package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.TokenError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StartState(override val context: User, private val token: String?) :
  BotState<StudentId?, Unit, StudentApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: StudentApi,
  ): Result<StudentId?, FrontendError> = coroutineBinding {
    runCatching {
        val id = service.loginByTgId(context.id).bind()?.id
        if (id != null) {
          if (token != null) {
            service
              .registerForCourseWithToken(token, id)
              .mapBoth(
                success = { course ->
                  bot.send(context, Dialogues.successfullyRegisteredForCourse(course, token))
                },
                failure = { error ->
                  when (error) {
                    else -> {
                      val deepError = error.error
                      if (deepError is TokenError)
                        bot.send(context, Dialogues.failedToRegisterForCourse(deepError))
                      else if (!error.shouldBeIgnored) bot.send(context, error.toMessageText())
                    }
                  }
                },
              )
          }
        }
        return@runCatching id
      }
      .toTelegramError()
      .bind()
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: StudentId?,
  ): Result<Pair<State, Unit>, FrontendError> =
    if (input != null) {
        MenuState(context, input) to Unit
      } else {
        AskFirstNameState(context, token) to Unit
      }
      .ok()

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) =
    Unit.ok()
}
