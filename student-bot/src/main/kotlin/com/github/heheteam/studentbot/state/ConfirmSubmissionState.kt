package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.studentbot.StudentApi
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.map

class ConfirmSubmissionState(override val context: User, val solution: Solution) :
  BotState<Boolean, Boolean, StudentApi> {
  lateinit var solutionMessage: AccessibleMessage
  lateinit var confirmMessage: AccessibleMessage

  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi): Boolean {
    with(bot) {
      val confirmMessageKeyboard =
        buildColumnMenu(
          ButtonData("Да", "yes", { true }),
          ButtonData("Нет (отменить отправку)", "no", { false }),
        )
      solutionMessage = sendSolutionContent(context.id, solution.content)
      confirmMessage =
        sendMessage(
          context,
          "Вы уверены, что хотите отправить это решение?",
          replyMarkup = confirmMessageKeyboard.keyboard,
        )
      return waitDataCallbackQueryWithUser(context.id)
        .map { value: DataCallbackQuery -> confirmMessageKeyboard.handler.invoke(value.data).get() }
        .firstNotNull()
    }
  }

  override fun computeNewState(service: StudentApi, input: Boolean): Pair<State, Boolean> {
    if (input) {
      service.inputSolution(
        solution.studentId,
        solution.chatId,
        solution.messageId,
        solution.content,
        solution.problemId,
      )
    }
    return MenuState(context, solution.studentId) to input
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
}
