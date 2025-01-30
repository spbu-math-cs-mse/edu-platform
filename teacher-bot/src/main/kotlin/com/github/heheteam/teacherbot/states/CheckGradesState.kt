package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.queryPickerWithBackFromList
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.teacherbot.CoursesStatisticsResolver
import com.github.heheteam.teacherbot.Keyboards.returnBack
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

data class CourseGrades(
  val courseName: String,
  val gradedProblems: List<Pair<StudentId, Grade>>,
  val maxGrade: Grade,
)

class CheckGradesState(override val context: User, private val teacherId: TeacherId) :
  State, BotState<Course?, CourseGrades?, CoursesStatisticsResolver> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: CoursesStatisticsResolver,
  ): Course? {
    val courses = service.getAvailableCourses(teacherId)
    return queryCourse(bot, context, courses, "Выберите курс")
  }

  override suspend fun computeNewState(
    service: CoursesStatisticsResolver,
    input: Course?,
  ): Pair<BotState<*, *, *>, CourseGrades?> {
    val course = input ?: return Pair(MenuState(context, teacherId), null)
    val gradedProblems = service.getGrading(course)
    val maxGrade = service.getMaxGrade()
    return Pair(MenuState(context, teacherId), CourseGrades(course.name, gradedProblems, maxGrade))
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesStatisticsResolver,
    response: CourseGrades?,
  ) {
    val grades = response ?: return
    val strGrades =
      "Оценки учеников на курсе ${grades.courseName}:\n" +
        grades.gradedProblems.withGradesToText(grades.maxGrade)
    respondWithGrades(bot, strGrades)
  }

  private suspend fun respondWithGrades(bot: BehaviourContext, strGrades: String) {
    val gradesMessage = bot.send(context, text = strGrades, replyMarkup = returnBack())
    bot.waitDataCallbackQueryWithUser(context.id).first()
    bot.deleteMessage(context, gradesMessage.messageId)
  }

  private suspend fun queryCourse(
    bot: BehaviourContext,
    user: User,
    courses: List<Course>,
    queryText: String,
  ): Course? = bot.run { queryPickerWithBackFromList(user, courses, { it.name }, queryText) }

  private fun List<Pair<StudentId, Grade?>>.withGradesToText(maxGrade: Grade) =
    joinToString(separator = "\n") { (studentId, grade) -> "Ученик $studentId : $grade/$maxGrade" }
}
