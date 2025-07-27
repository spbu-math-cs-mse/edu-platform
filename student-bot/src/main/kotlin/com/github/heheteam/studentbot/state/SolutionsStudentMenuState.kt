package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendVideo
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class SolutionsStudentMenuState(override val context: User, override val userId: StudentId) :
  BotStateWithHandlersAndStudentId<State, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = SolutionsStudentMenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<SuspendableBotAction, State, FrontendError>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val initialMessage =
      bot.send(
        context,
        text = StudentDialogues.solutionsIntro,
        replyMarkup = StudentKeyboards.solutionMenu(),
      )
    sentMessages.add(initialMessage)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      val state =
        when (dataCallbackQuery.data) {
          StudentKeyboards.FIRST_SOLUTION -> {
            val resourcePath = "/quiz-solution-1.mp4"
            sendVideoFromResource(resourcePath, bot)
            MenuState(context, userId)
          }
          StudentKeyboards.MENU -> {
            MenuState(context, userId)
          }
          else -> null
        }
      if (state != null) {
        UserInput(state)
      } else {
        Unhandled
      }
    }
  }

  private suspend fun sendVideoFromResource(resourcePath: String, bot: BehaviourContext) {
    val resouce = LocalMediaAttachment(AttachmentKind.PHOTO, resourcePath).openFile()
    val video = resouce.asMultipartFile()
    bot.sendVideo(context, video)
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: State,
  ): Result<Pair<State, Unit>, FrontendError> {
    return Pair(input, Unit).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
    input: State,
  ): Result<Unit, FrontendError> =
    runCatching { sentMessages.forEach { message -> bot.delete(message) } }.toTelegramError()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
