package com.github.heheteam.commonlib.quiz

import com.github.heheteam.commonlib.errors.EduPlatformResult
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.repository.CourseRepository
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class QuizService(
  private val quizRepository: QuizRepository,
  private val courseRepository: CourseRepository,
  private val studentBotTelegramController: StudentBotTelegramController,
  private val db: Database,
) {

  fun create(quizMetaInformation: QuizMetaInformation): EduPlatformResult<QuizId> =
    transaction(db) { quizRepository.saveQuiz(quizMetaInformation) }

  fun retrieve(courseId: CourseId): EduPlatformResult<List<RichQuiz>> =
    transaction(db) { quizRepository.findQuizzesFromCourse(courseId) }

  fun studentPerformanceOverview(
    studentId: StudentId,
    courseId: CourseId,
  ): EduPlatformResult<StudentOverCourseResults> =
    transaction(db) {
      binding {
        val quizzes = quizRepository.findQuizzesFromCourse(courseId).bind()
        var rightAnswers = 0
        for (quiz in quizzes) {
          val studentAnswer = quizRepository.getLastStudentAnswer(quiz.id, studentId).bind()
          if (
            studentAnswer != null &&
              studentAnswer.chosenAnswerIndex == quiz.metaInformation.correctAnswerIndex
          ) {
            rightAnswers++
          }
        }
        StudentOverCourseResults(totalQuizzes = quizzes.size, rightAnswers = rightAnswers)
      }
    }

  suspend fun activateQuiz(
    quizId: QuizId,
    currentTime: Instant,
  ): EduPlatformResult<QuizActivationResult> =
    newSuspendedTransaction(db = db) {
      coroutineBinding {
        val quiz = quizRepository.findQuizById(quizId).bind()
        if (quiz == null) {
          return@coroutineBinding QuizActivationResult.QuizNotFound
        }

        if (quiz.isActive) {
          return@coroutineBinding QuizActivationResult.QuizAlreadyActive
        }

        quizRepository.updateQuizActivationStatus(quizId, currentTime, true).bind()

        val course = courseRepository.findById(quiz.metaInformation.courseId).bind()
        for (studentId in course.students) {
          studentBotTelegramController
            .sendQuizActivation(
              courseId = quiz.metaInformation.courseId,
              quizId = quiz.id,
              questionText = quiz.metaInformation.questionText,
              answers = quiz.metaInformation.answers,
              duration = quiz.metaInformation.duration,
            )
            .bind()
        }

        QuizActivationResult.Success
      }
    }

  suspend fun updateQuizzesStati(currentTime: Instant): EduPlatformResult<Unit> =
    newSuspendedTransaction(db = db) {
      coroutineBinding {
        val allQuizzes = quizRepository.findAllQuizzes().bind()
        for (quiz in allQuizzes) {
          val status = quiz.tryDeactivate(currentTime)
          if (status == QuizDeactivationStatus.Deactivated) {
            quizRepository.updateQuizActivationStatus(quiz.id, null, false).bind()

            val course = courseRepository.findById(quiz.metaInformation.courseId).bind()
            for (studentId in course.students) {
              val studentAnswer = quizRepository.getLastStudentAnswer(quiz.id, studentId).bind()
              val score =
                if (
                  studentAnswer != null &&
                    studentAnswer.chosenAnswerIndex == quiz.metaInformation.correctAnswerIndex
                )
                  1
                else 0
              studentBotTelegramController
                .notifyOnPollQuizEnd(
                  studentId = studentId,
                  quizId = quiz.id,
                  chosenAnswerIndex = studentAnswer?.chosenAnswerIndex,
                  correctAnswerIndex = quiz.metaInformation.correctAnswerIndex,
                  score = score,
                )
                .bind()
            }
          }
        }
        Ok(Unit)
      }
    }

  fun storeStudentAnswer(
    quizId: QuizId,
    studentId: StudentId,
    chosenAnswerIndex: Int,
  ): EduPlatformResult<Unit> =
    transaction(db) { quizRepository.storeStudentAnswer(quizId, studentId, chosenAnswerIndex) }

  fun processStudentAnswer(
    quizId: QuizId,
    studentId: StudentId,
    chosenAnswerIndex: Int,
  ): EduPlatformResult<AnswerQuizResult> =
    transaction(db) {
      binding {
        val quiz = quizRepository.findQuizById(quizId).bind()
        if (quiz == null) {
          return@binding AnswerQuizResult.QuizNotFound
        }

        if (!quiz.isActive) {
          return@binding AnswerQuizResult.QuizInactive
        }
        val chosenAnswer = quiz.metaInformation.answers.getOrNull(chosenAnswerIndex)
        if (chosenAnswer == null) {
          return@binding AnswerQuizResult.QuizAnswerIndexOutOfBounds(
            chosenAnswerIndex,
            quiz.metaInformation.answers.lastIndex,
          )
        }

        quizRepository.storeStudentAnswer(quizId, studentId, chosenAnswerIndex).bind()
        AnswerQuizResult.Success(chosenAnswer)
      }
    }
}
