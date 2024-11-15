package com.github.heheteam.teacherbot.state

import com.github.heheteam.commonlib.*
import com.github.heheteam.teacherbot.*
import com.github.heheteam.teacherbot.Keyboards.returnBack
import com.github.heheteam.teacherbot.states.BotState
import com.github.heheteam.teacherbot.states.CheckGradesState
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.StartState
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first
import kotlin.random.Random

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(core: TeacherCore) {
  strictlyOn<CheckGradesState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val userId = core.getUserId(state.context.id)
    if (userId == null) {
      return@strictlyOn StartState(state.context)
    }

    // MOCK STUFF. Don't use in prod.
    // ---
    if (!wasMockGradeTableForTeacherBuilt) {
      wasMockGradeTableForTeacherBuilt = true
      val coursesProcessed = mutableSetOf<String>()
      for (studentCourses in mockStudentsAndCourses) {
        val student = mockStudentsTable[studentCourses.key]!!
        val courses = studentCourses.value

        for (courseId in courses) {
          if (!coursesProcessed.contains(courseId)) {
            coursesProcessed.add(courseId)
            if (Random.nextBoolean()) {
              mockCoursesTable[courseId]!!.teachers.add(Teacher(userId))
            }
          }

          val course = mockCoursesTable[courseId]!!
          for (series in course.series) {
            for (problem in series.problems) {
              if (Random.nextBoolean()) {
                core.addAssessment(
                  student,
                  Teacher(if (course.teachers.map { it.id }.contains(userId)) userId else "0"),
                  Solution(
                    (mockIncrementalSolutionId++).toString(),
                    "",
                    RawChatId(0),
                    MessageId(0),
                    problem,
                    SolutionContent(),
                    SolutionType.TEXT,
                  ),
                  SolutionAssessment((0..problem.maxScore).random(), ""),
                )
              }
            }
          }
        }
      }
    }
    // TODO: use DB (course - user) in the future to fix this crap
    val courses =
      core
        .getGradeMap()
        .values
        .flatMap { it.keys }
        .mapNotNull { it.getSeries() }
        .mapNotNull { it.getCourse() }
        .filter { it -> it.teachers.map { it.id }.contains(userId) }
        .associateBy { it.id }
    // ---

    val chooseCourseMessage =
      bot.send(
        state.context,
        text = "Выберите курс",
        replyMarkup =
        InlineKeyboardMarkup(
          keyboard =
          matrix {
            courses.forEach { row { dataButton(it.value.description, "courseId ${it.value.id}") } }
            row { dataButton("Назад \uD83D\uDD19", returnBack) }
          },
        ),
      )

    val callback = waitDataCallbackQuery().first()
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

      val gradeMap = core.getGradeMap()

      val maxGrade =
        series
          .flatMap { paper ->
            paper.problems
          }.sumOf { it.maxScore }

      gradeMap.forEach { studentMapEntry ->
        val student = studentMapEntry.key
        val solvedProblems = studentMapEntry.value
        val grade =
          solvedProblems
            .filter { (problem: Problem, _: Grade) ->
              series.map { it.id }.contains(problem.seriesId)
            }.map { (_: Problem, grade: Grade) -> grade }
            .sum()

        strGrades +=
          "${if (student.name.isEmpty() || student.surname.isEmpty()) "Ученик ${student.id}" else "${student.name} ${student.surname}"}: $grade/$maxGrade"
        strGrades += "\n"
      }
      strGrades.dropLast(1)

      val gradesMessage =
        bot.send(
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
