package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.metaData.back
import com.github.heheteam.studentbot.queryCourse
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(core: StudentCore) {
  strictlyOn<CheckGradesState> { state ->
    val courses = core.getStudentCourses(state.studentId)
    val courseId: CourseId =
      queryCourse(state, courses, "Выберите курс")?.id
        ?: return@strictlyOn MenuState(state.context, state.studentId)
    val assignmentsFromCourse = core.getCourseAssignments(courseId)
    val queryGradeType = queryGradeTypeKeyboard(state, assignmentsFromCourse, core, courseId)
    val sendQueryMessage =
      bot.send(
        state.context,
        text = "Какие оценки посмотреть?",
        replyMarkup = queryGradeType.keyboard,
      )
    val callback = waitDataCallbackQueryWithUser(state.context.id).first()
    deleteMessage(sendQueryMessage)
    val nextState = queryGradeType.handler(callback.data)
    nextState ?: MenuState(state.context, state.studentId)
  }
}

private fun BehaviourContext.queryGradeTypeKeyboard(
  state: CheckGradesState,
  assignmentsFromCourse: List<Assignment>,
  core: StudentCore,
  courseId: CourseId,
) =
  buildColumnMenu(
    ButtonData("Моя успеваемость", ButtonKey.STUDENT_GRADES) {
      val assignmentId =
        queryAssignmentFromUser(state, assignmentsFromCourse)
          ?: return@ButtonData CheckGradesState(state.context, state.studentId)
      val assignment = assignmentsFromCourse.find { it.id == assignmentId }!!
      val gradedProblems = core.getGradingForAssignment(assignment.id, state.studentId)
      respondWithGrades(state, assignment, gradedProblems)
      MenuState(state.context, state.studentId)
    },
    ButtonData("Лучшие на курсе", ButtonKey.TOP_GRADES) {
      val topGrades = core.getTopGrades(courseId)
      respondWithTopGrades(state, topGrades)
      MenuState(state.context, state.studentId)
    },
    ButtonData("Назад", ButtonKey.BACK) { CheckGradesState(state.context, state.studentId) },
  )

private suspend fun BehaviourContext.respondWithGrades(
  state: CheckGradesState,
  assignment: Assignment,
  gradedProblems: List<Pair<Problem, Grade?>>,
) {
  val strGrades = "Оценки за серию ${assignment.description}:\n" + gradedProblems.withGradesToText()

  val gradesMessage = bot.send(state.context, text = strGrades, replyMarkup = back())
  waitDataCallbackQueryWithUser(state.context.id).first()
  deleteMessage(gradesMessage)
}

private suspend fun BehaviourContext.queryAssignmentFromUser(
  state: CheckGradesState,
  assignments: List<Assignment>,
): AssignmentId? {
  val chooseAssignmentMessage =
    bot.send(
      state.context,
      text = "Выберите серию",
      replyMarkup =
        InlineKeyboardMarkup(
          keyboard =
            matrix {
              assignments.forEach {
                row { dataButton(it.description, "${ButtonKey.ASSIGNMENT_ID} ${it.id}") }
              }
              row { dataButton("Назад", ButtonKey.BACK) }
            }
        ),
    )

  val callback = waitDataCallbackQueryWithUser(state.context.id).first()
  deleteMessage(chooseAssignmentMessage)
  val assignmentId =
    when {
      callback.data.contains(ButtonKey.ASSIGNMENT_ID) -> {
        callback.data.split(" ").last().toLong()
      }

      else -> return null
    }
  return AssignmentId(assignmentId)
}

private suspend fun BehaviourContext.respondWithTopGrades(
  state: CheckGradesState,
  topGrades: List<Grade>,
) {
  if (topGrades.isEmpty()) {
    val gradesMessage =
      bot.send(state.context, text = "Еще никто не получал оценок на курсе!", replyMarkup = back())

    waitDataCallbackQueryWithUser(state.context.id).first()
    deleteMessage(gradesMessage)

    return
  }

  val strTopGrades = "Лучшие результаты на курсе:\n" + topGrades.withTopGradesToText()

  val gradesMessage = bot.send(state.context, text = strTopGrades, replyMarkup = back())
  waitDataCallbackQueryWithUser(state.context.id).first()
  deleteMessage(gradesMessage)
}

fun List<Pair<Problem, Grade?>>.withGradesToText() =
  joinToString(separator = "\n") { (problem, grade) ->
    "№${problem.number} — " +
      when {
        grade == null -> "не сдано"
        grade <= 0 -> "❌ 0/${problem.maxScore}"
        grade < problem.maxScore -> "\uD83D\uDD36 $grade/${problem.maxScore}"
        else -> "✅ $grade/${problem.maxScore}"
      }
  }

fun List<Grade>.withTopGradesToText() =
  this.withIndex().joinToString(separator = "\n") { (index, grade) ->
    when (index) {
      0 -> "\uD83E\uDD47 $grade"
      1 -> "\uD83E\uDD48 $grade"
      2 -> "\uD83E\uDD49 $grade"
      else -> "${index + 1}. $grade"
    }
  }
