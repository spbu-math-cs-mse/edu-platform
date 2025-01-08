package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.queryPickerWithBackFromList
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.teacherbot.Keyboards.returnBack
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckGradesState(core: TeacherCore) {
  strictlyOn<CheckGradesState> { state ->
    val courses = core.getAvailableCourses(state.teacherId)

    val course =
      queryCourse(state.context, courses, "Выберите курс")
        ?: return@strictlyOn MenuState(state.context, state.teacherId)
    val gradedProblems = core.getGrading(course)
    val maxGrade = core.getMaxGrade()
    val strGrades =
      "Оценки учеников на курсе ${course.name}:\n" + gradedProblems.withGradesToText(maxGrade)
    respondWithGrades(state, strGrades)
    MenuState(state.context, state.teacherId)
  }
}

private suspend fun BehaviourContext.respondWithGrades(state: CheckGradesState, strGrades: String) {
  val gradesMessage = bot.send(state.context, text = strGrades, replyMarkup = returnBack())
  waitDataCallbackQueryWithUser(state.context.id).first()
  deleteMessage(state.context.id, gradesMessage.messageId)
}

suspend fun BehaviourContext.queryCourse(
  user: User,
  courses: List<Course>,
  queryText: String,
): Course? = queryPickerWithBackFromList(user, courses, { it.name }, queryText)

fun List<Pair<StudentId, Grade?>>.withGradesToText(maxGrade: Grade) =
  joinToString(separator = "\n") { (studentId, grade) -> "Ученик $studentId : $grade/$maxGrade" }
