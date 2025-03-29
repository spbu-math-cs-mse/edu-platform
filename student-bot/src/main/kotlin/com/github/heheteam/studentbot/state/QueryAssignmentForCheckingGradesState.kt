package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemGrade
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotStateWithHandlers
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createAssignmentPicker
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.StudentApi
import com.github.heheteam.studentbot.metaData.back
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

data class QueryAssignmentForCheckingGradesState(
  override val context: User,
  val studentId: StudentId,
  val courseId: CourseId,
) :
  BotStateWithHandlers<
    Assignment?,
    Pair<Assignment, List<Pair<Problem, ProblemGrade>>>?,
    StudentApi,
  > {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Assignment?, Any>,
  ) {
    val assignments = service.getCourseAssignments(courseId)
    val coursesPicker = createAssignmentPicker(assignments)
    val message = bot.sendMessage(context.id, "Выберите курс", replyMarkup = coursesPicker.keyboard)
    sentMessages.add(message)
    bot.setMyCommands(BotCommand("menu", "main menu"))
    updateHandlersController.addTextMessageHandler { message ->
      if (message.content.text == "/menu") {
        NewState(MenuState(context, studentId))
      } else {
        Unhandled
      }
    }
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      coursesPicker
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override fun computeNewState(
    service: StudentApi,
    input: Assignment?,
  ): Pair<MenuState, Pair<Assignment, List<Pair<Problem, ProblemGrade>>>?> =
    if (input != null) {
      val gradedProblems = service.getGradingForAssignment(input.id, studentId)
      MenuState(context, studentId) to (input to gradedProblems)
    } else {
      MenuState(context, studentId) to null
    }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Pair<Assignment, List<Pair<Problem, ProblemGrade>>>?,
  ) {
    if (response != null) {
      bot.respondWithGrades(response.first, response.second)
    }
    for (message in sentMessages) {
      bot.delete(message)
    }
  }

  private suspend fun BehaviourContext.respondWithGrades(
    assignment: Assignment,
    gradedProblems: List<Pair<Problem, ProblemGrade>>,
  ) {
    val strGrades =
      "Оценки за серию ${assignment.description}:\n" + gradedProblems.withGradesToText()
    bot.send(context, text = strGrades, replyMarkup = back())
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
