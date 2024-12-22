package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitDocumentMessageWithUser
import com.github.heheteam.commonlib.util.waitMediaMessageWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.*
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.documentContentOrNull
import dev.inmo.tgbotapi.extensions.utils.mediaGroupContentOrNull
import dev.inmo.tgbotapi.extensions.utils.photoContentOrNull
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnSendSolutionState(
  core: StudentCore,
  studentBotToken: String,
) {
  strictlyOn<SendSolutionState> { state ->
    val studentId = state.studentId
    val courses = core.getStudentCourses(studentId)

    val stickerMessage = bot.sendSticker(state.context, Dialogues.nerdSticker)

    if (courses.isEmpty()) {
      suggestToApplyForCourses(state)
      return@strictlyOn MenuState(state.context, state.studentId)
    }

    val course = queryCourse(state, courses)
    if (course == null) {
      deleteMessage(stickerMessage)
      return@strictlyOn MenuState(state.context, state.studentId)
    }

    val assignments = core.getCourseAssignments(course.id)

    val problems = assignments.associateWith { core.getProblemsFromAssignment(it) }
    val problem = queryProblem(state, problems)
    if (problem == null) {
      deleteMessage(stickerMessage)
      return@strictlyOn MenuState(state.context, state.studentId)
    }

    state.selectedCourse = course

    var botMessage = bot.send(
      state.context,
      Dialogues.tellValidSolutionTypes(),
      replyMarkup = back(),
    )

    while (true) {
      val content =
        flowOf(
          waitDataCallbackQueryWithUser(state.context.id),
          waitTextMessageWithUser(state.context.id),
          waitMediaMessageWithUser(state.context.id),
          waitDocumentMessageWithUser(state.context.id),
        ).flattenMerge().first()

      if (content is DataCallbackQuery && content.data == ButtonKey.BACK) {
        deleteMessage(botMessage)
        deleteMessage(stickerMessage)
        return@strictlyOn SendSolutionState(state.context, state.studentId)
      }

      if (content is CommonMessage<*>) {
        val messageId = content.messageId

        val solutionContent = extractSolutionContent(content, studentBotToken)

        if (solutionContent == null) {
          deleteMessage(botMessage)
          botMessage = bot.send(
            state.context,
            Dialogues.tellSolutionTypeIsInvalid(),
            replyMarkup = back(),
          )
          continue
        }

        core.inputSolution(
          studentId,
          state.context.id.chatId,
          messageId,
          solutionContent,
          problem.id,
        )

        deleteMessage(botMessage)
        deleteMessage(stickerMessage)
        bot.sendSticker(state.context, Dialogues.okSticker)
        bot.send(state.context, Dialogues.tellSolutionIsSent())
        break
      }
    }
    MenuState(state.context, state.studentId)
  }
}

suspend fun BehaviourContext.makeURL(
  content: MediaContent,
  studentBotToken: String,
): String {
  val contentInfo = bot.getFileAdditionalInfo(content)
  return "https://api.telegram.org/file/bot$studentBotToken/${contentInfo.filePath}"
}

private suspend fun BehaviourContext.extractSolutionContent(
  content: CommonMessage<*>,
  studentBotToken: String,
): SolutionContent? {
  val textSolution = content.content.textContentOrNull()
  val photoSolution = content.content.photoContentOrNull()
  val groupSolution = content.content.mediaGroupContentOrNull()
  val documentSolution = content.content.documentContentOrNull()

  return if (textSolution != null) {
    return SolutionContent(text = textSolution.text, type = SolutionType.TEXT)
  } else if (photoSolution != null) {
    SolutionContent(
      filesURL = listOf(makeURL(photoSolution, studentBotToken)),
      type = SolutionType.PHOTO,
    )
  } else if (documentSolution != null) {
    SolutionContent(
      filesURL = listOf(makeURL(documentSolution, studentBotToken)),
      type = SolutionType.DOCUMENT,
    )
  } else if (groupSolution != null) {
    SolutionContent(
      filesURL = groupSolution.group.map {
        makeURL(
          it.content,
          studentBotToken,
        )
      },
      type = SolutionType.GROUP,
    )
  } else {
    null
  }
}

private suspend fun BehaviourContext.queryCourse(
  state: SendSolutionState,
  courses: List<Course>,
): Course? {
  val message =
    bot.send(
      state.context,
      Dialogues.askCourseForSolution(),
      replyMarkup = buildCoursesSendingSelector(courses),
    )

  val callbackData =
    waitDataCallbackQueryWithUser(state.context.id).first().data
  deleteMessage(message)

  if (callbackData == ButtonKey.BACK) {
    return null
  }

  val courseId = callbackData.split(" ").last()
  return courses.first { it.id == CourseId(courseId.toLong()) }
}

private suspend fun BehaviourContext.queryProblem(
  state: SendSolutionState,
  problems: Map<Assignment, List<Problem>>,
): Problem? {
  val message =
    bot.send(state.context, Dialogues.askProblem(), replyMarkup = buildProblemSendingSelector(problems))

  var callbackData = waitDataCallbackQueryWithUser(state.context.id).first().data
  while (callbackData == ButtonKey.FICTITIOUS) {
    callbackData = waitDataCallbackQueryWithUser(state.context.id).first().data
  }
  deleteMessage(message)

  if (callbackData == ButtonKey.BACK) {
    return null
  }

  val problemId = callbackData.split(" ").last()
  return problems.values.flatten().single { it.id == ProblemId(problemId.toLong()) }
}

private suspend fun BehaviourContext.suggestToApplyForCourses(state: SendSolutionState) {
  val message = bot.send(
    state.context,
    Dialogues.tellToApplyForCourses(),
    replyMarkup = back(),
  )
  waitDataCallbackQueryWithUser(state.context.id).first()
  deleteMessage(message)
}
