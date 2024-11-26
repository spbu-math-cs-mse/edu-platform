package com.github.heheteam.teacherbot.state

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.UserIdRegistry
import com.github.heheteam.teacherbot.*
import com.github.heheteam.teacherbot.Keyboards.returnBack
import com.github.heheteam.teacherbot.states.BotState
import com.github.heheteam.teacherbot.states.CheckGradesState
import com.github.heheteam.teacherbot.states.MenuState
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

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(
  userIdRegistry: UserIdRegistry,
  core: TeacherCore,
) {
  strictlyOn<CheckGradesState> { state ->
    val courses = core.getAvailableCourses(userIdRegistry.getUserId(state.context.id)!!)

    val courseId: String = queryCourseFromUser(state, courses)
      ?: return@strictlyOn MenuState(state.context)
    val course = courses.find { it.id == courseId }!!

    val gradedProblems = core.getGrading(course)
    val maxGrade = core.getMaxGrade(course)
    val strGrades = "Оценки учеников на курсе ${course.description}:\n" +
      gradedProblems
        .withGradesToText(maxGrade)
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
      replyMarkup = returnBack(),
    )
  waitDataCallbackQuery().first()
  deleteMessage(state.context.id, gradesMessage.messageId)
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
                "courseId ${it.id}",
              )
            }
          }
          row { dataButton("Назад", returnBack) }
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
  return courseId
}

fun List<Pair<Student, Grade?>>.withGradesToText(maxGrade: Grade) =
  joinToString(separator = "\n") { (student, grade) ->
    "${if (student.name.isEmpty() || student.surname.isEmpty()) "Ученик ${student.id}" else "${student.name} ${student.surname}"}: $grade/$maxGrade"
  }
