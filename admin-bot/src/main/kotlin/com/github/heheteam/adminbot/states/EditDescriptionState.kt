package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.newLine

class EditDescriptionState(
  override val context: User,
  val course: Course,
  val courseName: String,
  val adminId: AdminId,
) : BotStateWithHandlers<String, String?, AdminApi> {

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    bot.send(context) {
      +"Введите новое описание курса ${courseName}. Текущее описание:" + newLine + newLine
      +course.name
    }

    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, String?>, FrontendError> {
    val response =
      when {
        input == "/stop" -> null
        else -> {
          "Описание курса ${courseName} не обновлено, операция пока не поддержана"
        }
      }
    return Pair(MenuState(context, adminId), response).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: String?,
    input: String,
  ): Result<Unit, FrontendError> =
    runCatching { if (response != null) bot.send(context, response) }.toTelegramError()
}
