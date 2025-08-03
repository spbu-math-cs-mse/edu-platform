package com.github.heheteam.commonlib.integration.quiz

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.util.buildData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class StudentPerformanceTest : IntegrationTestEnvironment() {

  @Test
  fun `should correctly calculate student performance overview for multiple quizzes`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId1, courseId, _, studentIds) =
        setupQuizScenario(
          quizMeta = defaultQuizMeta,
          activationTime = at(10.seconds),
          studentCount = 2,
        )
      val (quizId2, _, _, _) =
        setupQuizScenario(
          quizMeta = anotherQuizMeta,
          activationTime = at(10.seconds),
          studentCount = 0,
        ) // Create another quiz, but don't add studentsi to it via setup

      val student1Id = studentIds[0]
      val student2Id = studentIds[1]

      apis.studentApi.answerQuiz(quizId1, student1Id, 1).value
      apis.studentApi.answerQuiz(quizId2, student1Id, 0).value

      apis.studentApi.answerQuiz(quizId1, student2Id, 0).value
      apis.studentApi.answerQuiz(quizId2, student2Id, 2).value

      val student1Results = apis.studentApi.getStudentQuizPerformance(student1Id, courseId).value
      assertEquals(2, student1Results.totalQuizzes)
      assertEquals(1, student1Results.rightAnswers)

      val student2Results = apis.studentApi.getStudentQuizPerformance(student2Id, courseId).value
      assertEquals(2, student2Results.totalQuizzes)
      assertEquals(1, student2Results.rightAnswers)
    }
  }

  @Test
  fun `should return zero quizzes and right answers for a course with no quizzes`() = runTest {
    buildData(createDefaultApis()) {
      val teacher = teacher("Teacher1", "Teacher1", 100L)
      val student = student("Student1", "Student1", 200L)
      val course =
        course("Course1") {
          withTeacher(teacher)
          withStudent(student)
        }

      val studentResults = apis.studentApi.getStudentQuizPerformance(student.id, course.id).value
      assertEquals(0, studentResults.totalQuizzes)
      assertEquals(0, studentResults.rightAnswers)
    }
  }

  @Test
  fun `should return zero right answers for a student who has not answered any quizzes`() =
    runTest {
      buildData(createDefaultApis()) {
        val (_, courseId, _, studentIds) =
          setupQuizScenario(activationTime = at(10.seconds), studentCount = 1)
        val studentId = studentIds[0]

        val studentResults = apis.studentApi.getStudentQuizPerformance(studentId, courseId).value
        assertEquals(1, studentResults.totalQuizzes)
        assertEquals(0, studentResults.rightAnswers)
      }
    }

  @Test
  fun `should return zero right answers for quizzes that were never activated`() = runTest {
    buildData(createDefaultApis()) {
      val (_, courseId, _, studentIds) = setupQuizScenario(activationTime = null, studentCount = 1)
      val studentId = studentIds[0]

      val studentResults = apis.studentApi.getStudentQuizPerformance(studentId, courseId).value
      assertEquals(1, studentResults.totalQuizzes)
      assertEquals(0, studentResults.rightAnswers)
    }
  }
}
