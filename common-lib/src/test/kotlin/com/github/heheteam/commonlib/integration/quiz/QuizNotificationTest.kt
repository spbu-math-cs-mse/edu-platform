package com.github.heheteam.commonlib.integration.quiz

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.util.buildData
import io.mockk.coVerify
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

class QuizNotificationTest : IntegrationTestEnvironment() {

  @Test
  fun `should send activation message to students upon quiz activation`() = runTest {
    buildData(createDefaultApis()) {
      val activationTime = at(10.seconds)
      val (quizId, courseId, _, _) = setupQuizScenario(activationTime = null, studentCount = 1)

      apis.teacherApi.activateQuiz(quizId, activationTime).value

      coVerify(exactly = 1) {
        studentBotController.sendQuizActivation(
          courseId = courseId,
          quizId = quizId,
          questionText = defaultQuizMeta.questionText,
          answers = defaultQuizMeta.answers,
          duration = defaultQuizMeta.duration,
        )
      }
    }
  }

  @Test
  fun `should send correct telegram activation message content`() = runTest {
    buildData(createDefaultApis()) {
      val customQuizMeta =
        defaultQuizMeta.copy(
          questionText = "Custom Question?",
          answers = listOf("A", "B"),
          duration = 5.minutes,
        )
      val activationTime = at(10.seconds)
      val (quizId, courseId, _, _) =
        setupQuizScenario(quizMeta = customQuizMeta, activationTime = null, studentCount = 1)

      apis.teacherApi.activateQuiz(quizId, activationTime).value

      coVerify(exactly = 1) {
        studentBotController.sendQuizActivation(
          courseId = courseId,
          quizId = quizId,
          questionText = customQuizMeta.questionText,
          answers = customQuizMeta.answers,
          duration = customQuizMeta.duration,
        )
      }
    }
  }

  @Test
  fun `should send telegram messages upon quiz deactivation`() = runTest {
    buildData(createDefaultApis()) {
      val (quizId, _, _, studentIds) =
        setupQuizScenario(activationTime = at(10.seconds), studentCount = 2)
      val student1Id = studentIds[0]
      val student2Id = studentIds[1]

      apis.studentApi.answerQuiz(quizId, student1Id, 1).value
      apis.studentApi.answerQuiz(quizId, student2Id, 0).value

      apis.teacherApi.updateQuizzesStati(at(71.seconds)).value

      coVerify(exactly = 1) {
        studentBotController.notifyOnPollQuizEnd(
          studentId = student1Id,
          quizId = quizId,
          chosenAnswerIndex = 1,
          correctAnswerIndex = 1,
          score = 1,
        )
      }
      coVerify(exactly = 1) {
        studentBotController.notifyOnPollQuizEnd(
          studentId = student2Id,
          quizId = quizId,
          chosenAnswerIndex = 0,
          correctAnswerIndex = 1,
          score = 0,
        )
      }
    }
  }

  @Test
  fun `should send quiz end notification with zero score for student who did not answer`() =
    runTest {
      buildData(createDefaultApis()) {
        val (quizId, courseId, _, studentIds) =
          setupQuizScenario(activationTime = at(10.seconds), studentCount = 2)
        val student1Id = studentIds[0] // This student will answer
        val student2Id = studentIds[1] // This student will NOT answer

        studentApi.answerQuiz(quizId, student1Id, 1).value

        teacherApi.updateQuizzesStati(at(71.seconds)).value // Deactivate quiz

        coVerify(exactly = 1) {
          studentBotController.notifyOnPollQuizEnd(
            studentId = student1Id,
            quizId = quizId,
            chosenAnswerIndex = 1,
            correctAnswerIndex = 1,
            score = 1,
          )
        }
        coVerify(exactly = 1) {
          studentBotController.notifyOnPollQuizEnd(
            studentId = student2Id,
            quizId = quizId,
            chosenAnswerIndex = null, // Or a default indicating no answer
            correctAnswerIndex = 1,
            score = 0,
          )
        }
      }
    }
}
