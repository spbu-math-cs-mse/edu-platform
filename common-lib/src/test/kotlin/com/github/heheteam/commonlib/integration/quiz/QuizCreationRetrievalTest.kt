package com.github.heheteam.commonlib.integration.quiz

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.util.buildData
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class QuizCreationRetrievalTest : IntegrationTestEnvironment() {

  @Test
  fun `should retrieve a newly created quiz that is inactive and unactivated`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, courseId, _, _) = setupQuizScenario(studentCount = 0)

      val retrievedQuizzes = apis.teacherApi.retrieveQuizzes(courseId).value
      assertEquals(1, retrievedQuizzes.size)
      assertEquals(quizId, retrievedQuizzes[0].id)
      assertFalse(retrievedQuizzes[0].isActive)
      assertNull(retrievedQuizzes[0].metaInformation.activationTime)
    }
  }
}
