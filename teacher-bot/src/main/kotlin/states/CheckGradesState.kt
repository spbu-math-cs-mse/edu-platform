package com.github.heheteam.samplebot.state

import Grade
import GradeTable
import Keyboards.returnBack
import Problem
import com.github.heheteam.samplebot.buildMock
import com.github.heheteam.samplebot.getCourse
import com.github.heheteam.samplebot.getSeries
import com.github.heheteam.samplebot.mockTeachers
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import states.BotState
import states.CheckGradesState
import states.MenuState
import states.StartState

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(
  gradeTable: GradeTable,
) {
  strictlyOn<CheckGradesState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val username = state.context.username!!.username
    if (!mockTeachers.containsKey(username)) {
      return@strictlyOn StartState(state.context)
    }
    val teacherId = state.context.id.toString()

    // MOCK STUFF. Don't use in prod.
    // ---
    gradeTable.buildMock(teacherId)
    val courses =
      gradeTable.getGradeMap().values.flatMap { it.keys }.mapNotNull { it.getSeries() }.mapNotNull { it.getCourse() }
        .filter { it -> it.teachers.map { it.id }.contains(teacherId) }.associateBy { it.id }
    // ---

    val chooseCourseMessage = bot.send(
      state.context,
      text = "Выберите курс",
      replyMarkup = InlineKeyboardMarkup(
        keyboard = matrix {
          courses.forEach { row { dataButton(it.value.description, "courseId ${it.value.id}") } }
          row { dataButton("Назад \uD83D\uDD19", returnBack) }
        },
      ),
    )

    var callback = waitDataCallbackQuery().first()
    deleteMessage(state.context.id, chooseCourseMessage.messageId)
    var courseId: String? = null
    when {
      callback.data.contains("courseId") -> {
        courseId = callback.data.split(" ").last()
      }
    }

    if (courseId != null) {
      val series = courses[courseId]!!.series
      var strGrades = "Оценки учеников на курсе ${courses[courseId]!!.description}:\n"

      val gradeMap = gradeTable.getGradeMap()

      val maxGrade = series.flatMap { paper ->
        paper.problems
      }.sumOf { it.maxScore }

      gradeMap.forEach { studentMapEntry ->
        val student = studentMapEntry.key
        val solvedProblems = studentMapEntry.value
        val grade = solvedProblems.filter { (problem: Problem, grade: Grade) ->
          series.map { it.id }.contains(problem.seriesId)
        }.map { (problem: Problem, grade: Grade) -> grade }.sum()

        strGrades += "${if (student.name.isEmpty() || student.surname.isEmpty()) "Ученик ${student.id}" else "${student.name} ${student.surname}"}: $grade/$maxGrade"
        strGrades += "\n"
      }
      strGrades.dropLast(1)

      val gradesMessage = bot.send(
        state.context,
        text = strGrades,
        replyMarkup = returnBack(),
      )

      waitDataCallbackQuery().first()
      deleteMessage(state.context.id, gradesMessage.messageId)
    }

    MenuState(state.context)
  }
}
