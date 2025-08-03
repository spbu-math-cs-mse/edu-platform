package com.github.heheteam.commonlib.integration.quiz

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.quiz.AnswerQuizResult
import com.github.heheteam.commonlib.util.buildData
import io.mockk.coVerify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class StudentAnswerTest : IntegrationTestEnvironment() {

  @Test
  fun `should overwrite previous student answer for the same quiz`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, _, _, studentIds) =
        setupQuizScenario(activationTime = at(10.seconds), studentCount = 1)
      val studentId = studentIds[0]
      apis.studentApi.answerQuiz(quizId, studentId, 0).value
      apis.studentApi.answerQuiz(quizId, studentId, 1).value // Overwrite
      apis.teacherApi.updateQuizzesStati(at(71.seconds)).value // Deactivate quiz
      coVerify(exactly = 1) {
        studentBotController.notifyOnPollQuizEnd(
          studentChat(1),
          quizId,
          1,
          defaultQuizMeta.correctAnswerIndex,
          any(),
        )
      }
    }
  }

  @Test
  fun `should fail when student answers an inactive quiz`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, _, _, studentIds) = setupQuizScenario(activationTime = null, studentCount = 1)
      val studentId = studentIds[0]

      val result = apis.studentApi.answerQuiz(quizId, studentId, 0)
      assertIs<AnswerQuizResult.QuizInactive>(result.value)
    }
  }

  @Test
  fun `should fail when student answers a deactivated quiz`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, _, _, studentIds) =
        setupQuizScenario(activationTime = at(10.seconds), studentCount = 1)
      val studentId = studentIds[0]

      apis.teacherApi.updateQuizzesStati(at(71.seconds)).value // Deactivate quiz

      val result = apis.studentApi.answerQuiz(quizId, studentId, 0)
      assertIs<AnswerQuizResult.QuizInactive>(result.value)
    }
  }

  @Test
  fun `should fail when student answers with an invalid chosen answer index`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, _, _, studentIds) =
        setupQuizScenario(activationTime = at(10.seconds), studentCount = 1)
      val studentId = studentIds[0]

      val result = apis.studentApi.answerQuiz(quizId, studentId, 99) // Index out of bounds
      assertIs<AnswerQuizResult.QuizAnswerIndexOutOfBounds>(result.value)
    }
  }

  @Test
  fun `should return success with chosen answer when student answers an active quiz`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, _, _, studentIds) =
        setupQuizScenario(activationTime = at(10.seconds), studentCount = 1)
      val studentId = studentIds[0]
      val idx = 0
      val result = apis.studentApi.answerQuiz(quizId, studentId, idx).value
      assertIs<AnswerQuizResult.Success>(result)
      assertEquals(defaultQuizMeta.answers[idx], result.chosenAnswer)
    }
  }

  @Test
  fun `should return QuizNotFound when student answers a non-existent quiz`() = runTest {
    buildData(createDefaultApis()) {
      val student = student("John", "Doe")
      val nonExistentQuizId = QuizId(999L)

      val result = apis.studentApi.answerQuiz(nonExistentQuizId, student.id, 0)
      assertIs<AnswerQuizResult.QuizNotFound>(result.value)
    }
  }
}
