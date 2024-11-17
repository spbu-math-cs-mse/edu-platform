package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.*
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.metaData.back
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(core: StudentCore) {
  strictlyOn<CheckGradesState> { state ->
    val courses = core.getAvailableCourses()
    val courseId: String = queryCourseFromUser(state, courses)
      ?: return@strictlyOn MenuState(state.context)
    val course = courses.find { it.id == courseId }!!

    val assignmentsFromCourse = course.assignments
    val assignmentId = queryAssignmentFromUser(state, assignmentsFromCourse)
      ?: return@strictlyOn MenuState(state.context)
    val assignment = assignmentsFromCourse.find { it.id == assignmentId }!!

    val gradedProblems = core.getGradingForAssignment(assignment, course)
    val strGrades = "Оценки за серию ${assignment.description}:\n" +
      gradedProblems
        .withGradesToText()
    respondWithGrades(state, strGrades)
    MenuState(state.context)
  }
}

private suspend fun BehaviourContext.respondWithGrades(
  state: CheckGradesState,
  strGrades: String,
) {
  val gradesMessage =
    bot.send(
      state.context,
      text = strGrades,
      replyMarkup = back(),
    )
  waitDataCallbackQuery().first()
  deleteMessage(state.context.id, gradesMessage.messageId)
}

private suspend fun BehaviourContext.queryAssignmentFromUser(
  state: CheckGradesState,
  assignments: MutableList<Assignment>,
): String? {
  val chooseAssignmentMessage =
    bot.send(
      state.context,
      text = "Выберите серию",
      replyMarkup =
      InlineKeyboardMarkup(
        keyboard =
        matrix {
          assignments.forEach {
            row {
              dataButton(
                it.description,
                "${ButtonKey.ASSIGNMENT_ID} ${it.id}",
              )
            }
          }
          row { dataButton("Назад", ButtonKey.BACK) }
        },
      ),
    )

  val callback = waitDataCallbackQuery().first()
  deleteMessage(state.context.id, chooseAssignmentMessage.messageId)
  val assignmentId = when {
    callback.data.contains(ButtonKey.ASSIGNMENT_ID) -> {
      callback.data.split(" ").last()
    }

    else -> null
  }
  return assignmentId
}

private suspend fun BehaviourContext.queryCourseFromUser(
  state: CheckGradesState,
  courses: List<Course>,
): String? {
  val chooseCourseMessage =
    bot.send(
      state.context,
      text = "Выберите курс",
      replyMarkup =
      InlineKeyboardMarkup(
        keyboard =
        matrix {
          courses.forEach {
            row {
              dataButton(
                it.description,
                "${ButtonKey.COURSE_ID} ${it.id}",
              )
            }
          }
          row { dataButton("Назад", ButtonKey.BACK) }
        },
      ),
    )

  val callback = waitDataCallbackQuery().first()
  deleteMessage(state.context.id, chooseCourseMessage.messageId)
  var courseId: String? = null
  when {
    callback.data.contains(ButtonKey.COURSE_ID) -> {
      courseId = callback.data.split(" ").last()
    }
  }
  return courseId
}

fun List<Pair<Problem, Grade?>>.withGradesToText() =
  joinToString(separator = "\n") { (problem, grade) ->
    "№${problem.number} — " + when {
      grade == null -> "не сдано"
      grade <= 0 -> "❌ 0/${problem.maxScore}"
      grade < problem.maxScore -> "\uD83D\uDD36 $grade/${problem.maxScore}"
      else -> "✅ $grade/${problem.maxScore}"
    }
  }
