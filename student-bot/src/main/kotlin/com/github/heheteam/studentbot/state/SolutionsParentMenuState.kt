package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndParentId
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.state.parent.ParentMenuState
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
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class SolutionsParentMenuState(override val context: User, override val userId: ParentId) :
  BotStateWithHandlersAndParentId<State, Unit, ParentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = SolutionsParentMenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: ParentApi,
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
      val state = dataCallbackToState(dataCallbackQuery, bot)
      if (state != null) {
        UserInput(state)
      } else {
        Unhandled
      }
    }
  }

  @Suppress("MagicNumber")
  private suspend fun dataCallbackToState(
    dataCallbackQuery: DataCallbackQuery,
    bot: BehaviourContext,
  ): State? =
    when (dataCallbackQuery.data) {
      StudentKeyboards.SOLUTION1 -> handleSolution(bot, 1)
      StudentKeyboards.SOLUTION2 -> handleSolution(bot, 2)
      StudentKeyboards.SOLUTION3 -> handleSolution(bot, 3)
      StudentKeyboards.SOLUTION4 -> handleSolution(bot, 4)
      StudentKeyboards.SOLUTION5 -> handleSolution(bot, 5)
      StudentKeyboards.SOLUTION6 -> handleSolution(bot, 6)

      StudentKeyboards.MENU -> {
        ParentMenuState(context, userId)
      }

      else -> null
    }

  private suspend fun handleSolution(bot: BehaviourContext, i: Int): State {
    val resourcePath = "/quiz-solution-$i.mp4"
    sendResourceFromPath(resourcePath, bot)
    return ParentMenuState(context, userId)
  }

  private suspend fun sendResourceFromPath(resourcePath: String, bot: BehaviourContext) {
    val resouce = LocalMediaAttachment(AttachmentKind.PHOTO, resourcePath).openFile()
    val video = resouce.asMultipartFile()
    bot.sendVideo(context, video)
  }

  override suspend fun computeNewState(
    service: ParentApi,
    input: State,
  ): Result<Pair<State, Unit>, FrontendError> {
    return Pair(input, Unit).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ParentApi,
    response: Unit,
    input: State,
  ): Result<Unit, FrontendError> =
    runCatching { sentMessages.forEach { message -> bot.delete(message) } }.toTelegramError()

  override suspend fun outro(bot: BehaviourContext, service: ParentApi) = Unit
}
