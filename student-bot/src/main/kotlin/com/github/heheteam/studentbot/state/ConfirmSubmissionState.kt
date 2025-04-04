package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.sendTextWithMediaAttachments
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class ConfirmSubmissionState(
  override val context: User,
  val solutionInputRequest: SolutionInputRequest,
) : BotStateWithHandlers<Boolean, Boolean, StudentApi> {
  lateinit var solutionMessage: AccessibleMessage
  lateinit var confirmMessage: AccessibleMessage

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Boolean, Any>,
  ) {

    val confirmMessageKeyboard =
      buildColumnMenu(
        ButtonData("Да", "yes") { true },
        ButtonData("Нет (отменить отправку)", "no") { false },
      )
    solutionMessage =
      bot.sendTextWithMediaAttachments(context.id, solutionInputRequest.solutionContent)
    confirmMessage =
      bot.sendMessage(
        context,
        "Вы уверены, что хотите отправить это решение?",
        replyMarkup = confirmMessageKeyboard.keyboard,
      )
    bot.setMyCommands(BotCommand("menu", "main menu"))
    updateHandlersController.addTextMessageHandler { message ->
      if (message.content.text == "/menu") {
        NewState(MenuState(context, solutionInputRequest.studentId))
      } else {
        Unhandled
      }
    }
    updateHandlersController.addDataCallbackHandler { value: DataCallbackQuery ->
      val result = confirmMessageKeyboard.handler.invoke(value.data)
      result.mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override fun computeNewState(service: StudentApi, input: Boolean): Pair<State, Boolean> {
    if (input) {
      service.inputSolution(solutionInputRequest)
    }
    return MenuState(context, solutionInputRequest.studentId) to input
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Boolean) {
    with(bot) {
      delete(solutionMessage)
      delete(confirmMessage)
    }
    if (response) {
      bot.send(context.id, "Решение успешно отправлено на проверку!")
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
