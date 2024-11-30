package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentIdRegistry
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.*
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDocumentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMediaMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.documentContentOrNull
import dev.inmo.tgbotapi.extensions.utils.mediaGroupContentOrNull
import dev.inmo.tgbotapi.extensions.utils.photoContentOrNull
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSendSolutionState(
  userIdRegistry: StudentIdRegistry,
  core: StudentCore,
) {
  strictlyOn<SendSolutionState> { state ->
    val studentId = userIdRegistry.getUserId(state.context.id)!!
    val courses = core.getStudentCourses(studentId)

    val stickerMessage = bot.sendSticker(state.context, Dialogues.nerdSticker)

    if (courses.isEmpty()) {
      suggestToApplyForCourses(state)
      return@strictlyOn MenuState(state.context)
    }

    val course = queryCourse(state, courses)
    if (course == null) {
      deleteMessage(stickerMessage)
      return@strictlyOn MenuState(state.context)
    }

    val assignments = core.getCourseAssignments(course.id)

    val assignment = queryAssignments(state, assignments)
    if (assignment == null) {
      deleteMessage(stickerMessage)
      return@strictlyOn MenuState(state.context)
    }

    val problems = core.getProblemsFromAssignment(assignment)
    val problem = queryProblem(state, problems)
    if (problem == null) {
      deleteMessage(stickerMessage)
      return@strictlyOn MenuState(state.context)
    }

    state.selectedCourse = course

    var botMessage = bot.send(state.context, Dialogues.tellValidSolutionTypes(), replyMarkup = back())

    while (true) {
      val content =
        flowOf(
          waitDataCallbackQuery(),
          waitTextMessage(),
          waitMediaMessage(),
          waitDocumentMessage(),
        ).flattenMerge().first()

      if (content is DataCallbackQuery && content.data == ButtonKey.BACK) {
        deleteMessage(botMessage)
        deleteMessage(stickerMessage)
        return@strictlyOn SendSolutionState(state.context)
      }

      if (content is CommonMessage<*>) {
        val messageId = content.messageId

        val solutionContent = extractSolutionContent(content)

        if (solutionContent == null) {
          deleteMessage(botMessage)
          botMessage = bot.send(state.context, Dialogues.tellSolutionTypeIsInvalid(), replyMarkup = back())
          continue
        }
        core.inputSolution(studentId, state.context.id.chatId, messageId, solutionContent, problem.id)

        deleteMessage(botMessage)
        deleteMessage(stickerMessage)
        bot.sendSticker(state.context, Dialogues.okSticker)
        bot.send(state.context, Dialogues.tellSolutionIsSent())
        break
      }
    }
    MenuState(state.context)
  }
}

private fun extractSolutionContent(content: CommonMessage<*>): SolutionContent? {
  val textSolution = content.content.textContentOrNull()
  val photoSolution = content.content.photoContentOrNull()
  val photosSolution =
    content.content.mediaGroupContentOrNull()?.group?.mapNotNull {
      it.content.photoContentOrNull() ?: it.content.documentContentOrNull()
    }
  val documentSolution = content.content.documentContentOrNull()

  return if (textSolution != null) {
    SolutionContent(text = textSolution.text)
  } else if (photoSolution != null) {
    SolutionContent(text = SolutionType.PHOTO.toString(), fileIds = listOf(photoSolution.media.fileId.fileId))
  } else if (photosSolution != null) {
    SolutionContent(text = SolutionType.PHOTOS.toString(), fileIds = photosSolution.map { it.media.fileId.fileId })
  } else if (documentSolution != null) {
    SolutionContent(text = SolutionType.DOCUMENT.toString(), fileIds = listOf(documentSolution.media.fileId.fileId))
  } else {
    null
  }
}

private suspend fun BehaviourContext.queryCourse(
  state: SendSolutionState,
  courses: List<Course>,
): Course? {
  val message =
    bot.send(state.context, Dialogues.askCourseForSolution(), replyMarkup = buildCoursesSendingSelector(courses))

  val callbackData = waitDataCallbackQuery().first().data
  deleteMessage(message)

  if (callbackData == ButtonKey.BACK) {
    return null
  }

  val courseId = callbackData.split(" ").last()
  return courses.first { it.id == CourseId(courseId.toLong()) }
}

private suspend fun BehaviourContext.queryAssignments(
  state: SendSolutionState,
  assignments: List<Assignment>,
): Assignment? {
  val message =
    bot.send(state.context, Dialogues.askAssignmentFromSolution(), replyMarkup = buildAssignmentSendingSelector(assignments))

  val callbackData = waitDataCallbackQuery().first().data
  deleteMessage(message)

  if (callbackData == ButtonKey.BACK) {
    return null
  }

  val assignmentId = callbackData.split(" ").last()
  return assignments.first { it.id == AssignmentId(assignmentId.toLong()) }
}

private suspend fun BehaviourContext.queryProblem(
  state: SendSolutionState,
  problems: List<Problem>,
): Problem? {
  val message =
    bot.send(state.context, Dialogues.askAssignmentFromSolution(), replyMarkup = buildProblemSendingSelector(problems))

  val callbackData = waitDataCallbackQuery().first().data
  deleteMessage(message)

  if (callbackData == ButtonKey.BACK) {
    return null
  }

  val problemId = callbackData.split(" ").last()
  return problems.single { it.id == ProblemId(problemId.toLong()) }
}

private suspend fun BehaviourContext.suggestToApplyForCourses(state: SendSolutionState) {
  val message = bot.send(state.context, Dialogues.tellToApplyForCourses(), replyMarkup = back())
  waitDataCallbackQuery().first()
  deleteMessage(message)
}
