package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInput
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.HandlingError
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.heheteam.studentbot.Keyboards.CHECK_DEADLINES
import com.github.heheteam.studentbot.Keyboards.CHECK_GRADES
import com.github.heheteam.studentbot.Keyboards.SEND_SOLUTION
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

data class MenuState(override val context: User, override val userId: StudentId) :
  BotStateWithHandlersAndStudentId<State, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, State, Any>,
  ) {
    val stickerMessage = bot.sendSticker(context.id, Dialogues.typingSticker)
    val initialMessage = bot.send(context, text = Dialogues.menu(), replyMarkup = Keyboards.menu())
    sentMessages.add(stickerMessage)
    sentMessages.add(initialMessage)
    updateHandlersController.addDataCallbackHandler(::processKeyboardButtonPresses)
    updateHandlersController.addTextMessageHandler { t -> bot.handleTextMessage(t, context) }
  }

  private fun processKeyboardButtonPresses(
    callback: DataCallbackQuery
  ): HandlerResultWithUserInputOrUnhandled<Nothing, State, Nothing> {
    val state =
      when (callback.data) {
        SEND_SOLUTION -> QueryCourseForSolutionSendingState(context, userId)
        CHECK_GRADES -> QueryCourseForSolutionSendingState(context, userId)
        CHECK_DEADLINES -> QueryCourseForCheckingDeadlinesState(context, userId)
        else -> null
      }
    return if (state != null) {
      UserInput(state)
    } else {
      Unhandled
    }
  }

  override fun computeNewState(service: StudentApi, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) {
    for (message in sentMessages) {
      bot.delete(message)
    }
  }

  private fun BehaviourContext.handleTextMessage(
    t: CommonMessage<TextContent>,
    user: User,
  ): HandlerResultWithUserInput<Nothing, Nothing, String> {
    val re = Regex("/setid ([0-9]+)")
    val match = re.matchEntire(t.content.text)
    return if (match != null) {
      val newIdStr = match.groups[1]?.value ?: return HandlingError("bad regex (blame programmers)")
      val newId =
        newIdStr.toLongOrNull()
          ?: run {
            logger.error("input id $newIdStr is not long!")
            return HandlingError("Input id is not long")
          }
      NewState(PresetStudentState(user, newId.toStudentId()))
    } else {
      HandlingError("Unrecognized command")
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
