package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createAssignmentPicker
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.logger
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

data class QueryAssignmentForCheckingGradesState(
  override val context: User,
  override val userId: StudentId,
  val courseId: CourseId,
  val assignments: List<Assignment>,
) :
  BotStateWithHandlersAndStudentId<
    Assignment?,
    Pair<Assignment, Result<List<Pair<Problem, ProblemGrade>>, EduPlatformError>>?,
    StudentApi,
  > {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Assignment?, Any>,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val assignments = service.getCourseAssignments(courseId).bind()
    val coursesPicker = createAssignmentPicker(assignments)
    val selectCourseMessage =
      bot.send(context.id, "Выберите серию", replyMarkup = coursesPicker.keyboard)
    sentMessages.add(selectCourseMessage)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      coursesPicker
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: Assignment?,
  ): Pair<State, Pair<Assignment, Result<List<Pair<Problem, ProblemGrade>>, EduPlatformError>>?> =
    if (input != null) {
      val gradedProblems = service.getGradingForAssignment(input.id, userId)
      MenuState(context, userId) to (input to gradedProblems)
    } else {
      MenuState(context, userId) to input
    }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Pair<Assignment, Result<List<Pair<Problem, ProblemGrade>>, EduPlatformError>>?,
  ) {
    if (response != null) {
      val grades = response.second
      grades.mapBoth(
        success = { gradedProblems -> bot.respondWithGrades(response.first, gradedProblems) },
        failure = {
          val errorMessage = "Ошибка! Не получилось запросить ваши оценки"
          bot.send(context, text = errorMessage)
        },
      )
    }
    for (message in sentMessages) {
      runCatching { bot.delete(message) }
        .mapError { logger.warning("Message $message delete failed", it) }
    }
  }

  private suspend fun BehaviourContext.respondWithGrades(
    assignment: Assignment,
    gradedProblems: List<Pair<Problem, ProblemGrade>>,
  ) {
    val strGrades =
      "Оценки за серию ${assignment.description}:\n" + gradedProblems.withGradesToText()
    bot.send(context, text = strGrades)
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}

private fun List<Pair<Problem, ProblemGrade>>.withGradesToText(): String =
  joinToString(separator = "\n") { (problem, grade) ->
    "№${problem.number} — " +
      when (grade) {
        is ProblemGrade.Unsent -> "не сдано"
        is ProblemGrade.Unchecked -> "🕊 не проверено"
        is ProblemGrade.Graded ->
          when {
            grade.grade <= 0 -> "❌ 0/${problem.maxScore}"
            grade.grade < problem.maxScore -> "\uD83D\uDD36 $grade/${problem.maxScore}"
            else -> "✅ $grade/${problem.maxScore}"
          }
      }
  }
