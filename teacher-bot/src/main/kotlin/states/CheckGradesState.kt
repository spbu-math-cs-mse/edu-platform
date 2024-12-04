package com.github.heheteam.teacherbot.state

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherIdRegistry
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.teacherbot.*
import com.github.heheteam.teacherbot.Keyboards.returnBack
import com.github.heheteam.teacherbot.states.BotState
import com.github.heheteam.teacherbot.states.CheckGradesState
import com.github.heheteam.teacherbot.states.MenuState
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(
  userIdRegistry: TeacherIdRegistry,
  core: TeacherCore,
) {
  strictlyOn<CheckGradesState> { state ->
    val courses =
      core.getAvailableCourses(userIdRegistry.getUserId(state.context.id)!!)

    val courseId: CourseId =
      queryCourseFromUser(state, courses)
        ?: return@strictlyOn MenuState(state.context)
    val course = courses.find { it.id == courseId }!!

    val gradedProblems = core.getGrading(course)
    val maxGrade = core.getMaxGrade(course)
    val strGrades =
      "Оценки учеников на курсе ${course.name}:\n" +
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
  waitDataCallbackQueryWithUser(state.context.id).first()
  deleteMessage(state.context.id, gradesMessage.messageId)
}

private suspend fun BehaviourContext.queryCourseFromUser(
  state: CheckGradesState,
  courses: List<Course>,
): CourseId? {
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
                it.name,
                "courseId ${it.id}",
              )
            }
          }
          row { dataButton("Назад", returnBack) }
        },
      ),
    )

  val callback = waitDataCallbackQueryWithUser(state.context.id).first()
  deleteMessage(state.context.id, chooseCourseMessage.messageId)
  val courseId: String?
  when {
    callback.data.contains("courseId") -> {
      courseId = callback.data.split(" ").last()
    }

    else -> return null
  }
  return CourseId(courseId.toLong())
}

fun List<Pair<StudentId, Grade?>>.withGradesToText(maxGrade: Grade) =
  joinToString(separator = "\n") { (studentId, grade) ->
    "Ученик $studentId : $grade/$maxGrade"
  }
