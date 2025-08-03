package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.AdminKeyboards.RETURN_BACK
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.assignments.parseProblemsDescriptions
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.datetime.LocalDateTime

class CreateChallengeState(
  override val context: User,
  val adminId: AdminId,
  private val course: Course,
  private val assignmentId: AssignmentId,
  private var challengeDescription: Pair<String, LocalDateTime?>? = null,
  private var challengingProblems: List<ProblemDescription>? = null,
) : BotStateWithHandlers<State, Unit, AdminApi> {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()
  private var lastMessageId: MessageId? = null

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }

  @Suppress("UnsafeCallOnNullableType")
  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<State>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    when {
      challengeDescription == null -> handleChallengeDescription(bot, updateHandlersController)
      challengingProblems == null -> handleProblemsDescription(bot, updateHandlersController)
      else ->
        completeChallengeCreation(bot, service, challengeDescription!!, updateHandlersController)
    }
  }

  private suspend fun handleChallengeDescription(
    bot: BehaviourContext,
    updateHandlersController: UpdateHandlersController<SuspendableBotAction, State, FrontendError>,
  ) {
    val msg =
      bot.send(
        context,
        Dialogues.askChallengeDescription,
        replyMarkup = AdminKeyboards.returnBack(),
      )
    sentMessages.add(msg)
    lastMessageId = msg.messageId

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == RETURN_BACK) {
        NewState(MenuState(context, adminId))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context, adminId))
        else -> NewState(processDescriptionInput(message.content.text))
      }
    }
  }

  private fun processDescriptionInput(text: String): State {
    return if (text.contains("\$")) {
      val tokens = text.split("\$")
      if (tokens.size != 2) {
        CreateChallengeErrorState(
          context,
          course,
          assignmentId,
          "too many dollar signs in query",
          adminId,
        )
      } else {
        val date = LocalDateTime.Formats.ISO.parseOrNull(tokens[1])
        CreateChallengeState(context, adminId, course, assignmentId, tokens[0] to date, null)
      }
    } else {
      CreateChallengeState(context, adminId, course, assignmentId, text to null, null)
    }
  }

  private suspend fun handleProblemsDescription(
    bot: BehaviourContext,
    updateHandlersController: UpdateHandlersController<SuspendableBotAction, State, FrontendError>,
  ) {
    lastMessageId?.let {
      try {
        bot.delete(context.id, it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
    val msg =
      bot.send(
        context,
        Dialogues.askProblemsDescriptions,
        replyMarkup = AdminKeyboards.returnBack(),
      )
    sentMessages.add(msg as CommonMessage<TextContent>)
    lastMessageId = msg.messageId

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == RETURN_BACK) {
        NewState(MenuState(context, adminId))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addTextMessageHandler { message ->
      when (message.content.text) {
        "/menu" -> NewState(MenuState(context, adminId))
        "/stop" -> NewState(MenuState(context, adminId))
        else -> NewState(processProblemsInput(message.content.text))
      }
    }
  }

  private fun processProblemsInput(text: String): State {
    return parseProblemsDescriptions(text)
      .mapBoth(
        { result ->
          CreateChallengeState(context, adminId, course, assignmentId, challengeDescription, result)
        },
        { result -> CreateChallengeErrorState(context, course, assignmentId, result, adminId) },
      )
  }

  private suspend fun completeChallengeCreation(
    bot: BehaviourContext,
    service: AdminApi,
    challengeDescription: Pair<String, LocalDateTime?>,
    updateHandlersController: UpdateHandlersControllerDefault<State>,
  ) {
    service.createChallenge(
      course.id,
      assignmentId,
      challengeDescription.first,
      challengingProblems.orEmpty().map { it.copy(deadline = challengeDescription.second) },
    )
    bot.send(
      context,
      Dialogues.challengeWasCreatedSuccessfully,
      replyMarkup = inlineKeyboard { row { dataButton("Отлично!", "ok") } },
    )
    updateHandlersController.addDataCallbackHandler { NewState(MenuState(context, adminId)) }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: State,
  ): Result<Pair<State, Unit>, FrontendError> = Pair(input, Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: State,
  ): Result<Unit, FrontendError> = Unit.ok()
}
