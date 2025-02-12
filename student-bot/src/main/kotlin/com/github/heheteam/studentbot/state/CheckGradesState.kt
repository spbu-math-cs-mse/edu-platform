package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.queryAssignment
import com.github.heheteam.commonlib.util.queryCourse
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.Keyboards.STUDENT_GRADES
import com.github.heheteam.studentbot.Keyboards.TOP_GRADES
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.back
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

data class CheckGradesState(override val context: User, val studentId: StudentId) : State

fun DefaultBehaviourContextWithFSM<State>.strictlyOnCheckGradesState(core: StudentCore) {
  strictlyOn<CheckGradesState> { state ->
    val courses = core.getStudentCourses(state.studentId)
    val courseId: CourseId =
      queryCourse(state.context, courses)?.id
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
    nextState.get() ?: MenuState(state.context, state.studentId)
  }
}

private fun BehaviourContext.queryGradeTypeKeyboard(
  state: CheckGradesState,
  assignmentsFromCourse: List<Assignment>,
  core: StudentCore,
  courseId: CourseId,
) =
  buildColumnMenu(
    ButtonData("Моя успеваемость", STUDENT_GRADES) {
      val assignment =
        queryAssignment(state.context, assignmentsFromCourse)
          ?: return@ButtonData CheckGradesState(state.context, state.studentId)
      val gradedProblems = core.getGradingForAssignment(assignment.id, state.studentId)
      respondWithGrades(state, assignment, gradedProblems)
      MenuState(state.context, state.studentId)
    },
    ButtonData("Лучшие на курсе", TOP_GRADES) {
      val topGrades = core.getTopGrades(courseId)
      respondWithTopGrades(state, topGrades)
      MenuState(state.context, state.studentId)
    },
    ButtonData("Назад", RETURN_BACK) { CheckGradesState(state.context, state.studentId) },
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
