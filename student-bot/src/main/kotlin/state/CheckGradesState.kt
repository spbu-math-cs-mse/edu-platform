package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.*
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.metaData.back
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

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(core: StudentCore) {
  strictlyOn<CheckGradesState> { state ->
    val studentId = core.userIdRegistry.getUserId(state.context.id)!!
    val courses = core.coursesDistributor.getListOfCourses(studentId)
    // MOCK STUFF. Don't use in prod.
    // ---
    addRandomAssessments(courses, core, studentId)
    // ---

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
                  "${ButtonKey.COURSE_ID} ${it.id}"
                )
              }
            }
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
    if (courseId == null) {
      return@strictlyOn MenuState(state.context)
    }

    val assignments = courses.find { it.id == courseId }!!.assignments
    val assignmentId = run {
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
                    "${ButtonKey.ASSIGNMENT_ID} ${it.id}"
                  )
                }
              }
              row { dataButton("Назад", ButtonKey.BACK) }
            },
          ),
        )

      callback = waitDataCallbackQuery().first()
      deleteMessage(state.context.id, chooseAssignmentMessage.messageId)
      val assignmentId = when {
        callback.data.contains(ButtonKey.ASSIGNMENT_ID) -> {
          callback.data.split(" ").last()
        }

        else -> null
      }
      assignmentId
    }

    if (assignmentId != null) {
      val assignment = assignments.find { it.id == assignmentId }!!
      val grades = core.gradeTable.getGradeMap()[Student(studentId)]
        ?.filter { it.key.assignmentId == assignmentId }!!
      val strGrades = "Оценки за серию ${assignment.description}:\n" +
        assignment.problems.sortedBy { it.number }.withGradesToText(grades)

      val gradesMessage =
        bot.send(
          state.context,
          text = strGrades,
          replyMarkup = back(),
        )

      waitDataCallbackQuery().first()
      deleteMessage(state.context.id, gradesMessage.messageId)
    }

    MenuState(state.context)
  }
}

fun List<Problem>.withGradesToText(grades: Map<Problem, Grade>) =
  joinToString(separator = "\n") { problem ->
    val grade = grades[problem]
    "№${problem.number} — " + when {
      grade == null -> "не сдано"
      grade <= 0 -> "❌ 0/${problem.maxScore}"
      grade < problem.maxScore -> "\uD83D\uDD36 $grade/${problem.maxScore}"
      else -> "✅ $grade/${problem.maxScore}"
    }
  }


private fun addRandomAssessments(
  courses: List<Course>,
  core: StudentCore,
  studentId: String
) {
  for (problem in courses.flatMap { it.assignments }.flatMap { it.problems }) {
    if (Random.nextBoolean()) {
      core.gradeTable.addAssessment(
        Student(studentId),
        Teacher("0"),
        Solution(
          (mockIncrementalSolutionId++).toString(),
          "",
          RawChatId(0),
          MessageId(0),
          problem,
          SolutionContent(),
          SolutionType.TEXT
        ),
        SolutionAssessment((0..problem.maxScore).random(), ""),
      )
    }
  }
}
