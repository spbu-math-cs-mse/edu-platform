package com.github.heheteam.samplebot.state

import GradeTable
import Student
import com.github.heheteam.samplebot.data.CoursesDistributor
import com.github.heheteam.samplebot.data.buildMock
import com.github.heheteam.samplebot.metaData.ButtonKey
import com.github.heheteam.samplebot.metaData.back
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(
  coursesDistributor: CoursesDistributor,
  gradeTable: GradeTable,
) {
  strictlyOn<CheckGradesState> { state ->
    val studentId = state.context.id.toString()
    val courses = coursesDistributor.getListOfCourses(studentId)
    // MOCK STUFF. Don't use in prod.
    // ---
    gradeTable.buildMock(studentId, coursesDistributor)
    // ---

    val chooseCourseMessage = bot.send(
      state.context,
      text = "Выберите курс",
      replyMarkup = InlineKeyboardMarkup(
        keyboard = matrix {
          courses.forEach { row { dataButton(it.description, "${ButtonKey.COURSE_ID} ${it.id}") } }
          row { dataButton("Назад", ButtonKey.BACK) }
        },
      ),
    )

    var callback = waitDataCallbackQuery().first()
    deleteMessage(state.context.id, chooseCourseMessage.messageId)
    var courseId: String? = null
    when {
      callback.data.contains(ButtonKey.COURSE_ID) -> {
        courseId = callback.data.split(" ").last()
      }
    }

    if (courseId != null) {
      val series = courses.find { it.id == courseId }!!.series

      val chooseSeriesMessage = bot.send(
        state.context,
        text = "Выберите серию",
        replyMarkup = InlineKeyboardMarkup(
          keyboard = matrix {
            series.forEach { row { dataButton(it.description, "${ButtonKey.SERIES_ID} ${it.id}") } }
            row { dataButton("Назад", ButtonKey.BACK) }
          },
        ),
      )

      callback = waitDataCallbackQuery().first()
      deleteMessage(state.context.id, chooseSeriesMessage.messageId)
      var seriesId: String? = null
      when {
        callback.data.contains(ButtonKey.SERIES_ID) -> {
          seriesId = callback.data.split(" ").last()
        }
      }

      if (seriesId != null) {
        val exactSeries = series.find { it.id == seriesId }!!
        val grades = if (!gradeTable.getGradeMap().containsKey(Student(studentId))) {
          mapOf()
        } else {
          gradeTable.getGradeMap()[Student(studentId)]!!.filter { it.key.seriesId == seriesId }
        }

        var strGrades = "Оценки за серию ${exactSeries.description}:\n"
        for (problem in exactSeries.problems.sortedBy { it.number }) {
          val grade = grades[problem]
          strGrades += "№${problem.number} — " + if (grade == null) {
            "➖ не сдано"
          } else {
            when {
              grade <= 0 -> "❌ 0/${problem.maxScore}"
              grade < problem.maxScore -> "\uD83D\uDD36 $grade/${problem.maxScore}"
              else -> "✅ $grade/${problem.maxScore}"
            }
          }
          strGrades += "\n"
        }
        strGrades.dropLast(1)

        val gradesMessage = bot.send(
          state.context,
          text = strGrades,
          replyMarkup = back(),
        )

        waitDataCallbackQuery().first()
        deleteMessage(state.context.id, gradesMessage.messageId)
      }
    }

    MenuState(state.context)
  }
}
