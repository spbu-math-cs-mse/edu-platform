package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Dialogues.askAssignmentDescription
import com.github.heheteam.adminbot.Dialogues.askCourse
import com.github.heheteam.adminbot.Dialogues.askProblemsDescriptions
import com.github.heheteam.adminbot.Dialogues.assignmentDescriptionIsNotText
import com.github.heheteam.adminbot.Dialogues.assignmentWasCreatedSuccessfully
import com.github.heheteam.adminbot.Dialogues.incorrectProblemDescriptionEmpty
import com.github.heheteam.adminbot.Dialogues.incorrectProblemDescriptionMaxScoreIsNotInt
import com.github.heheteam.adminbot.Dialogues.incorrectProblemDescriptionTooManyArguments
import com.github.heheteam.adminbot.Dialogues.noCoursesWasFoundForCreationOfAssignment
import com.github.heheteam.adminbot.Dialogues.problemsDescriptionsAreNotTexts
import com.github.heheteam.adminbot.Keyboards.buildCoursesSelector
import com.github.heheteam.adminbot.Keyboards.returnBack
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCreateAssignmentState(core: AdminCore) {
  strictlyOn<CreateAssignmentState> { state ->
    val courses = core.getCourses().values.toList()
    if (courses.isEmpty()) {
      bot.send(state.context, text = noCoursesWasFoundForCreationOfAssignment())
      return@strictlyOn MenuState(state.context)
    }

    val course = queryCourse(state, courses) ?: return@strictlyOn MenuState(state.context)
    val description =
      queryAssignmentDescription(state) ?: return@strictlyOn MenuState(state.context)
    val problemsDescriptions =
      queryProblemsDescriptions(state) ?: return@strictlyOn MenuState(state.context)

    core.addAssignment(course.id, description, problemsDescriptions)
    bot.send(state.context, assignmentWasCreatedSuccessfully())
    MenuState(state.context)
  }
}

private suspend fun BehaviourContext.queryCourse(
  state: CreateAssignmentState,
  courses: List<Course>,
): Course? {
  val message = bot.send(state.context, askCourse(), replyMarkup = buildCoursesSelector(courses))

  val callbackData = waitDataCallbackQueryWithUser(state.context.id).first().data
  deleteMessage(message)

  if (callbackData == returnBack) {
    return null
  }

  val courseId = callbackData.split(" ").last()
  return courses.first { it.id == CourseId(courseId.toLong()) }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun BehaviourContext.queryAssignmentDescription(
  state: CreateAssignmentState
): String? {
  val messages: MutableList<ContentMessage<TextContent>> = mutableListOf()
  messages.add(
    bot.send(state.context, text = askAssignmentDescription(), replyMarkup = returnBack())
  )

  while (true) {
    when (
      val response =
        flowOf(
            waitDataCallbackQueryWithUser(state.context.id),
            waitTextMessageWithUser(state.context.id),
          )
          .flattenMerge()
          .first()
    ) {
      is DataCallbackQuery -> {
        if (response.data == returnBack) {
          messages.forEach { delete(it) }
          return null
        }
      }

      is CommonMessage<*> -> {
        val descriptionFromText = response.content.textContentOrNull()?.text
        if (descriptionFromText == null) {
          editMessageReplyMarkup(messages.last(), replyMarkup = null)
          messages.add(
            bot.send(state.context, assignmentDescriptionIsNotText(), replyMarkup = returnBack())
          )
          continue
        }
        messages.forEach { delete(it) }
        return descriptionFromText
      }
    }
  }
}

private suspend fun BehaviourContext.queryProblemsDescriptions(
  state: CreateAssignmentState
): List<ProblemDescription>? {
  val messages =
    mutableListOf<ContentMessage<TextContent>>().apply {
      add(bot.send(state.context, text = askProblemsDescriptions(), replyMarkup = returnBack()))
    }
  val problemsDescriptions: List<ProblemDescription>
  while (true) {
    when (
      val response =
        merge(
            waitDataCallbackQueryWithUser(state.context.id),
            waitTextMessageWithUser(state.context.id),
          )
          .first()
    ) {
      is DataCallbackQuery -> {
        if (response.data == returnBack) {
          messages.forEach { delete(it) }
          return null
        }
      }

      is CommonMessage<*> -> {
        val problemsDescriptionsFromText = response.content.textContentOrNull()?.text
        if (problemsDescriptionsFromText == null) {
          handleError(state, messages, problemsDescriptionsAreNotTexts())
          continue
        }

        val parsedProblemsDescriptions = parseProblemsDescriptions(problemsDescriptionsFromText)
        if (parsedProblemsDescriptions.isErr) {
          handleError(state, messages, parsedProblemsDescriptions.error)
          continue
        }
        problemsDescriptions = parsedProblemsDescriptions.value
        break
      }
    }
  }
  messages.forEach { delete(it) }
  return problemsDescriptions
}

private suspend fun BehaviourContext.handleError(
  state: CreateAssignmentState,
  messages: MutableList<ContentMessage<TextContent>>,
  errorMessage: String,
) {
  editMessageReplyMarkup(messages.last(), replyMarkup = null)
  messages.add(bot.send(state.context, errorMessage, replyMarkup = returnBack()))
}

internal fun parseProblemsDescriptions(
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
