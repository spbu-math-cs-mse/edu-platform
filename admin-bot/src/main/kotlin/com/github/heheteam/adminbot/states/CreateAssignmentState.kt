package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.AdminKeyboards.RETURN_BACK
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.datetime.LocalDateTime

class CreateAssignmentState(
  override val context: User,
  private val course: Course,
  private var description: Pair<String, LocalDateTime?>? = null,
  private var problems: List<ProblemDescription>? = null,
) : BotStateWithHandlers<State, Unit, AdminApi> {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()
  private var lastMessageId: MessageId? = null

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersController<BehaviourContext.() -> Unit, State, Any>,
  ) {
    when {
      description == null -> handleAssignmentDescription(bot, updateHandlersController)
      problems == null -> handleProblemsDescription(bot, updateHandlersController)
      else -> completeAssignmentCreation(bot, service, description!!)
    }
  }

  private suspend fun handleAssignmentDescription(
    bot: BehaviourContext,
    updateHandlersController: UpdateHandlersController<BehaviourContext.() -> Unit, State, Any>,
  ) {
    val msg =
      bot.send(
        context,
        Dialogues.askAssignmentDescription(),
        replyMarkup = AdminKeyboards.returnBack(),
      )
    sentMessages.add(msg)
    lastMessageId = msg.messageId

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == RETURN_BACK) {
        NewState(MenuState(context))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addTextMessageHandler { message ->
      when (message.content.text) {
        "/stop" -> NewState(MenuState(context))
        else -> NewState(processDescriptionInput(message.content.text))
      }
    }
  }

  private fun processDescriptionInput(text: String): State {
    return if (text.contains("\$")) {
      val tokens = text.split("\$")
      if (tokens.size != 2) {
        CreateAssignmentErrorState(context, course, "too many dollar signs in query")
      } else {
        val date = LocalDateTime.Formats.ISO.parseOrNull(tokens[1])
        CreateAssignmentState(context, course, tokens[0] to date, null)
      }
    } else {
      CreateAssignmentState(context, course, text to null, null)
    }
  }

  private suspend fun handleProblemsDescription(
    bot: BehaviourContext,
    updateHandlersController: UpdateHandlersController<BehaviourContext.() -> Unit, State, Any>,
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
        Dialogues.askProblemsDescriptions(),
        replyMarkup = AdminKeyboards.returnBack(),
      )
    sentMessages.add(msg as CommonMessage<TextContent>)
    lastMessageId = msg.messageId

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == RETURN_BACK) {
        NewState(MenuState(context))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addTextMessageHandler { message ->
      when (message.content.text) {
        "/menu" -> NewState(MenuState(context))
        "/stop" -> NewState(MenuState(context))
        else -> NewState(processProblemsInput(message.content.text))
      }
    }
  }

  private fun processProblemsInput(text: String): State {
    return parseProblemsDescriptions(text)
      .mapBoth(
        { result -> CreateAssignmentState(context, course, description, result) },
        { result -> CreateAssignmentErrorState(context, course, result) },
      )
  }

  private suspend fun completeAssignmentCreation(
    bot: BehaviourContext,
    service: AdminApi,
    assignmentDescription: Pair<String, LocalDateTime?>,
  ) {
    service.createAssignment(
      course.id,
      assignmentDescription.first,
      problems.orEmpty().map { it.copy(deadline = assignmentDescription.second) },
    )
    bot.send(context, Dialogues.assignmentWasCreatedSuccessfully())
    NewState(MenuState(context))
  }

  override fun computeNewState(service: AdminApi, input: State) = Pair(input, Unit)

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: Unit) = Unit
}

fun parseProblemsDescriptions(
  problemsDescriptionsFromText: String
): Result<List<ProblemDescription>, String> {
  val problemsDescriptions = mutableListOf<ProblemDescription>()
  problemsDescriptionsFromText.lines().mapIndexed { index, problemDescription ->
    val arguments =
      """[^\s"]+|"([^"]*)""""
        .toRegex()
        .findAll(problemDescription)
        .map { it.groups[1]?.value ?: it.value }
        .toList()

    val maxScore =
      when {
        arguments.isEmpty() -> {
          Err(Dialogues.incorrectProblemDescriptionEmpty())
        }

        arguments.size > 3 -> {
          Err(Dialogues.incorrectProblemDescriptionTooManyArguments(problemDescription))
        }

        else -> {
          arguments
            .elementAtOrElse(2) { "1" }
            .toIntOrNull()
            .toResultOr { Dialogues.incorrectProblemDescriptionMaxScoreIsNotInt(arguments.last()) }
        }
      }
    if (maxScore.isErr) {
      return Err(maxScore.error)
    }
    problemsDescriptions.add(
      ProblemDescription(
        index + 1,
        arguments.first(),
        arguments.elementAtOrElse(1) { "" },
        maxScore.value,
      )
    )
  }
  return Ok(problemsDescriptions)
}
