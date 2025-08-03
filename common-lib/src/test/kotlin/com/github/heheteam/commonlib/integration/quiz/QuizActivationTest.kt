package com.github.heheteam.commonlib.integration.quiz

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.quiz.QuizActivationResult
import com.github.heheteam.commonlib.util.buildData
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class QuizActivationTest : IntegrationTestEnvironment() {

  @Test
  fun `should activate a quiz successfully`() = runTest {
    buildData(createDefaultApis()) {
      val activationTime = at(10.seconds)
      val (quizId, courseId, _, _) = setupQuizScenario(activationTime = null, studentCount = 1)

      val activationResult = apis.teacherApi.activateQuiz(quizId, activationTime).value
      assertIs<QuizActivationResult.Success>(activationResult)

      val retrievedQuizzes = apis.teacherApi.retrieveQuizzes(courseId).value
      assertEquals(1, retrievedQuizzes.size)
      assertTrue(retrievedQuizzes[0].isActive)
      assertEquals(activationTime, retrievedQuizzes[0].metaInformation.activationTime)
    }
  }

  @Test
  fun `should fail to activate an already active quiz`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, _, _, _) = setupQuizScenario(activationTime = at(10.seconds))

      val result = apis.teacherApi.activateQuiz(quizId, at(20.seconds))
      assertIs<QuizActivationResult.QuizAlreadyActive>(result.value)
    }
  }

  @Test
  fun `should fail to activate a non-existent quiz`() = runTest {
    buildData(createDefaultApis()) {
      val result = apis.teacherApi.activateQuiz(QuizId(999L), at(0.seconds))
      assertIs<QuizActivationResult.QuizNotFound>(result.value)
    }
  }
}
