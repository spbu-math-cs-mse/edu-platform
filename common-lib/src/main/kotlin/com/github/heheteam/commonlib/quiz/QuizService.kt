package com.github.heheteam.commonlib.quiz

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.domain.RichCourse
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.EduPlatformResult
import com.github.heheteam.commonlib.errors.toStackedString
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.repository.CourseRepository
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.CoroutineBindingScope
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.onFailure
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("LongParameterList") // it is not too long
class QuizService
internal constructor(
  private val quizRepository: QuizRepository,
  private val courseRepository: CourseRepository,
  private val studentBotTelegramController: StudentBotTelegramController,
  private val teacherBotTelegramController: TeacherBotTelegramController,
  private val db: Database,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
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
        val quiz =
          quizRepository.findQuizById(quizId).bind()
            ?: return@coroutineBinding QuizActivationResult.QuizNotFound

        if (quiz.isActive) {
          return@coroutineBinding QuizActivationResult.QuizAlreadyActive
        }

        quizRepository.updateQuizActivationStatus(quizId, currentTime, true).bind()
        val loadedQuiz =
          quizRepository.findQuizById(quizId).bind()
            ?: return@coroutineBinding QuizActivationResult.QuizNotFound
        println(loadedQuiz.isActive)
        val course = courseRepository.findById(quiz.metaInformation.courseId).bind()
        for (studentId in course.students) {
          val student = studentStorage.resolveStudent(studentId).bind() ?: continue
          studentBotTelegramController
            .sendQuizActivation(
              rawChatId = student.tgId,
              quizId = quiz.id,
              questionText = quiz.metaInformation.questionText,
              answers = quiz.metaInformation.answers,
              duration = quiz.metaInformation.duration,
            )
            .onFailure {
              logger.error { "Failed to send quiz activation message. ${it.toStackedString()}" }
            }
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
            sendStatusToTeacher(quiz, course)

            for (studentId in course.students) {
              val student = studentStorage.resolveStudent(studentId).bind() ?: continue
              sendStatusToStudent(quiz, studentId, student)
            }
          }
        }
        Ok(Unit)
      }
    }

  private suspend fun CoroutineBindingScope<EduPlatformError>.sendStatusToTeacher(
    quiz: RichQuiz,
    course: RichCourse,
  ) {
    val quizOverallResults = quiz.getQuizOverallResults(course.students)
    val teacher = teacherStorage.resolveTeacher(quiz.metaInformation.teacherId).bind()
    teacherBotTelegramController
      .sendQuizOverallResult(
        chatId = teacher.tgId,
        questionText = quiz.metaInformation.questionText,
        totalParticipants = quizOverallResults.totalParticipants,
        correctAnswers = quizOverallResults.correctAnswers,
        incorrectAnswers = quizOverallResults.incorrectAnswers,
        notAnswered = quizOverallResults.notAnswered,
      )
      .onFailure {
        logger.error { "Failed to send quiz overall result to teacher: ${it.toStackedString()}" }
      }
  }

  private suspend fun CoroutineBindingScope<EduPlatformError>.sendStatusToStudent(
    quiz: RichQuiz,
    studentId: StudentId,
    student: Student,
  ) {
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
        chatId = student.tgId,
        quizId = quiz.id,
        chosenAnswerIndex = studentAnswer?.chosenAnswerIndex,
        correctAnswerIndex = quiz.metaInformation.correctAnswerIndex,
        score = score,
      )
      .onFailure {
        logger.error("Не получилось отправить результат ученику; ${it.toStackedString()}")
      }
  }

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
