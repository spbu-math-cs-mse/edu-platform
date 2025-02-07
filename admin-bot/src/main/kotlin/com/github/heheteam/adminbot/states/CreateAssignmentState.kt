package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AssignmentCreator
import com.github.heheteam.adminbot.Dialogues.askAssignmentDescription
import com.github.heheteam.adminbot.Dialogues.askProblemsDescriptions
import com.github.heheteam.adminbot.Dialogues.assignmentDescriptionIsNotText
import com.github.heheteam.adminbot.Dialogues.assignmentWasCreatedSuccessfully
import com.github.heheteam.adminbot.Dialogues.incorrectProblemDescriptionEmpty
import com.github.heheteam.adminbot.Dialogues.incorrectProblemDescriptionMaxScoreIsNotInt
import com.github.heheteam.adminbot.Dialogues.incorrectProblemDescriptionTooManyArguments
import com.github.heheteam.adminbot.Dialogues.problemsDescriptionsAreNotTexts
import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.Keyboards.RETURN_BACK
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.LocalDateTime

class CreateAssignmentState(override val context: User, private val course: Course) :
  BotState<State, Unit, AssignmentCreator> {
  override suspend fun readUserInput(bot: BehaviourContext, service: AssignmentCreator): State {
    val description = queryAssignmentDescription(bot, this) ?: return MenuState(context)
    val problemsDescriptions = queryProblemsDescriptions(bot, this) ?: return MenuState(context)

    service.createAssignment(
      course.id,
      description.first,
      problemsDescriptions.map { it.copy(deadline = description.second) },
    )
    bot.send(context, assignmentWasCreatedSuccessfully())
    return MenuState(context)
  }

  override fun computeNewState(service: AssignmentCreator, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AssignmentCreator,
    response: Unit,
  ) = Unit

  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun queryAssignmentDescription(
    bot: BehaviourContext,
    state: CreateAssignmentState,
  ): Pair<String, LocalDateTime?>? {
    val messages: MutableList<ContentMessage<TextContent>> = mutableListOf()
    messages.add(
      bot.send(
        state.context,
        text = askAssignmentDescription(),
        replyMarkup = Keyboards.returnBack(),
      )
    )

    while (true) {
      when (
        val response =
          flowOf(
              bot.waitDataCallbackQueryWithUser(state.context.id),
              bot.waitTextMessageWithUser(state.context.id),
            )
            .flattenMerge()
            .first()
      ) {
        is DataCallbackQuery -> {
          if (response.data == RETURN_BACK) {
            messages.forEach { bot.delete(it) }
            return null
          }
        }

        is CommonMessage<*> -> {
          val descriptionFromText = response.content.textContentOrNull()?.text
          if (descriptionFromText == null) {
            bot.editMessageReplyMarkup(messages.last(), replyMarkup = null)
            messages.add(
              bot.send(
                state.context,
                assignmentDescriptionIsNotText(),
                replyMarkup = Keyboards.returnBack(),
              )
            )
            continue
          }
          messages.forEach { bot.delete(it) }
          if (descriptionFromText.contains("\$")) {
            val tokens = descriptionFromText.split("\$")
            if (tokens.size != 2) {
              bot.logger.info("too many dollar signs in query")
              return null
            } else {
              val before = tokens[0]
              val after = tokens[1]
              return before to LocalDateTime.Formats.ISO.parseOrNull(after)
            }
          } else {
            return descriptionFromText to null
          }
        }
      }
    }
  }

  private suspend fun queryProblemsDescriptions(
    bot: BehaviourContext,
    state: CreateAssignmentState,
  ): List<ProblemDescription>? {
    val messages =
      mutableListOf<ContentMessage<TextContent>>().apply {
        add(
          bot.send(
            state.context,
            text = askProblemsDescriptions(),
            replyMarkup = Keyboards.returnBack(),
          )
        )
      }
    val problemsDescriptions: List<ProblemDescription>
    while (true) {
      when (
        val response =
          merge(
              bot.waitDataCallbackQueryWithUser(state.context.id),
              bot.waitTextMessageWithUser(state.context.id),
            )
            .first()
      ) {
        is DataCallbackQuery -> {
          if (response.data == RETURN_BACK) {
            messages.forEach { bot.delete(it) }
            return null
          }
        }

        is CommonMessage<*> -> {
          val problemsDescriptionsFromText = response.content.textContentOrNull()?.text
          if (problemsDescriptionsFromText == null) {
            handleError(bot, state, messages, problemsDescriptionsAreNotTexts())
            continue
          }

          val parsedProblemsDescriptions = parseProblemsDescriptions(problemsDescriptionsFromText)
          if (parsedProblemsDescriptions.isErr) {
            handleError(bot, state, messages, parsedProblemsDescriptions.error)
            continue
          }
          problemsDescriptions = parsedProblemsDescriptions.value
          break
        }
      }
    }
    messages.forEach { bot.delete(it) }
    return problemsDescriptions
  }

  private suspend fun handleError(
    bot: BehaviourContext,
    state: CreateAssignmentState,
    messages: MutableList<ContentMessage<TextContent>>,
    errorMessage: String,
  ) {
    bot.editMessageReplyMarkup(messages.last(), replyMarkup = null)
    messages.add(bot.send(state.context, errorMessage, replyMarkup = Keyboards.returnBack()))
  }
}

fun parseProblemsDescriptions(
  problemsDescriptionsFromText: String
): Result<List<ProblemDescription>, String> {
  val problemsDescriptions = mutableListOf<ProblemDescription>()
  for (problemDescription in problemsDescriptionsFromText.lines()) {
    val arguments =
      """[^\s"]+|"([^"]*)""""
        .toRegex()
        .findAll(problemDescription)
        .map { it.groups[1]?.value ?: it.value }
        .toList()

    when {
      arguments.isEmpty() -> {
        return Err(incorrectProblemDescriptionEmpty())
      }

      arguments.size > 3 -> {
        return Err(incorrectProblemDescriptionTooManyArguments(problemDescription))
      }

      else -> {
        val maxScore =
          arguments.elementAtOrElse(2) { "1" }.toIntOrNull()
            ?: return Err(incorrectProblemDescriptionMaxScoreIsNotInt(arguments.last()))
        problemsDescriptions.add(
          ProblemDescription(arguments.first(), arguments.elementAtOrElse(1) { "" }, maxScore)
        )
      }
    }
  }
  return Ok(problemsDescriptions)
}
