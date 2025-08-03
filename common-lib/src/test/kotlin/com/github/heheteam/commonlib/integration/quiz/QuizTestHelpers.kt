package com.github.heheteam.commonlib.integration.quiz

import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.quiz.QuizMetaInformation
import com.github.heheteam.commonlib.quiz.RichQuiz
import com.github.heheteam.commonlib.util.TestDataBuilder
import com.github.heheteam.commonlib.util.defaultInstant
import com.github.heheteam.commonlib.util.defaultTimezone
import com.github.heheteam.commonlib.util.toRawChatId
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

internal fun at(duration: Duration): Instant {
  return defaultInstant.plus(duration)
}

data class QuizScenarioContext(
  val quizId: QuizId,
  val courseId: CourseId,
  val teacherId: TeacherId,
  val studentIds: List<StudentId>,
)

val defaultQuizMeta =
  QuizMetaInformation(
    courseId = CourseId(0),
    teacherId = TeacherId(0),
    questionText = "What is 2+2?",
    answers = listOf("3", "4", "5"),
    correctAnswerIndex = 1,
    createdAt = at(0.minutes).toLocalDateTime(defaultTimezone),
    duration = 1.minutes,
  )

val anotherQuizMeta =
  QuizMetaInformation(
    courseId = CourseId(0),
    teacherId = TeacherId(0),
    questionText = "What is the capital of France?",
    answers = listOf("Berlin", "Madrid", "Paris", "Rome"),
    correctAnswerIndex = 2,
    createdAt = at(0.minutes).toLocalDateTime(defaultTimezone),
    duration = 2.minutes,
  )

internal val defaultTeacherChat = 100L.toRawChatId()

internal fun studentChat(i: Int) = (200L + i).toRawChatId()

suspend fun TestDataBuilder.setupQuizScenario(
  quizMeta: QuizMetaInformation = defaultQuizMeta,
  activationTime: Instant? = null,
  studentCount: Int = 1,
): QuizScenarioContext {
  val teacher = teacher("Teacher1", "Teacher1", defaultTeacherChat.long)
  val students = (1..studentCount).map { i -> student("Student$i", "Student$i", 200L + i) }
  val course =
    course("Course1") {
      withTeacher(teacher)
      students.forEach { s -> withStudent(s) }
    }

  val quizMetaWithIds = quizMeta.copy(courseId = course.id, teacherId = teacher.id)
  val quizId = apis.teacherApi.createQuiz(quizMetaWithIds).value

  if (activationTime != null) {
    apis.teacherApi.activateQuiz(quizId, activationTime).value
  }
  return QuizScenarioContext(quizId, course.id, teacher.id, students.map { it.id })
}

fun assertQuizState(quiz: RichQuiz, expectedActive: Boolean, expectedActivationTime: Instant?) {
  assertEquals(expectedActive, quiz.isActive)
  assertEquals(expectedActivationTime, quiz.metaInformation.activationTime)
}
