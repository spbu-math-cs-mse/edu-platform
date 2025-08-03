package com.github.heheteam.commonlib.integration.quiz

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.util.buildData
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class QuizDeactivationTest : IntegrationTestEnvironment() {

  @Test
  fun `should deactivate quiz after its duration passes`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, courseId, _, studentIds) =
        setupQuizScenario(activationTime = at(10.seconds), studentCount = 2)

      apis.studentApi.answerQuiz(quizId, studentIds[0], 1).value
      apis.studentApi.answerQuiz(quizId, studentIds[1], 0).value

      apis.teacherApi.updateQuizzesStati(at(71.seconds)).value

      val retrievedQuizzes = apis.teacherApi.retrieveQuizzes(courseId).value
      assertEquals(1, retrievedQuizzes.size)
      assertFalse(retrievedQuizzes[0].isActive)
    }
  }

  @Test
  fun `should not deactivate quiz before its duration passes`() = runTest {
    buildData(createDefaultApis()) {
      val (_, courseId, _, _) = setupQuizScenario(activationTime = at(10.seconds))

      apis.teacherApi
        .updateQuizzesStati(at(69.seconds))
        .value // Before 1 minute duration + 10 seconds activation

      val retrievedQuizzes = apis.teacherApi.retrieveQuizzes(courseId).value
      assertEquals(1, retrievedQuizzes.size)
      assertTrue(retrievedQuizzes[0].isActive)
    }
  }

  @Test
  fun `should deactivate multiple quizzes correctly based on their durations`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId1, courseId, _, _) =
        setupQuizScenario(
          quizMeta = defaultQuizMeta,
          activationTime = at(10.seconds),
        ) // 1 minute duration
      val (quizId2, _, _, _) =
        setupQuizScenario(
          quizMeta = anotherQuizMeta,
          activationTime = at(10.seconds),
        ) // 2 minutes duration

      apis.teacherApi.updateQuizzesStati(at(71.seconds)).value // After quiz1 duration, before quiz2

      val retrievedQuizzes = apis.teacherApi.retrieveQuizzes(courseId).value
      val quiz1 = retrievedQuizzes.find { it.id == quizId1 }
      val quiz2 = retrievedQuizzes.find { it.id == quizId2 }

      assertNotNull(quiz1)
      assertNotNull(quiz2)

      assertFalse(quiz1.isActive) // Should be deactivated
      assertTrue(quiz2.isActive) // Should still be active

      apis.teacherApi.updateQuizzesStati(at(131.seconds)).value // After quiz2 duration

      val retrievedQuizzesAfterSecondUpdate = apis.teacherApi.retrieveQuizzes(courseId).value
      val quiz1AfterSecondUpdate = retrievedQuizzesAfterSecondUpdate.find { it.id == quizId1 }
      val quiz2AfterSecondUpdate = retrievedQuizzesAfterSecondUpdate.find { it.id == quizId2 }

      assertNotNull(quiz1AfterSecondUpdate)
      assertNotNull(quiz2AfterSecondUpdate)

      assertFalse(quiz1AfterSecondUpdate.isActive)
      assertFalse(quiz2AfterSecondUpdate.isActive) // Should now be deactivated
    }
  }
}
