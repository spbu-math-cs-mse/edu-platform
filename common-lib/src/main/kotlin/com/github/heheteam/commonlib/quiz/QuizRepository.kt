package com.github.heheteam.commonlib.quiz

import com.github.heheteam.commonlib.database.table.QuizTable
import com.github.heheteam.commonlib.database.table.StudentAnswersTable
import com.github.heheteam.commonlib.errors.EduPlatformResult
import com.github.heheteam.commonlib.errors.NamedError
import com.github.heheteam.commonlib.errors.asEduPlatformError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class QuizRepository {

  fun findQuizzesFromCourse(courseId: CourseId): EduPlatformResult<List<RichQuiz>> =
    runCatching {
        val quizzes =
          QuizTable.selectAll()
            .where { QuizTable.courseId eq courseId.long }
            .map { row ->
              val quizId = QuizId(row[QuizTable.id].value)
              val studentAnswers =
                StudentAnswersTable.selectAll()
                  .where { StudentAnswersTable.quizId eq quizId.long }
                  .associate {
                    StudentId(it[StudentAnswersTable.studentId]) to
                      it[StudentAnswersTable.chosenAnswerIndex]
                  }

              val metaInformation =
                QuizMetaInformation(
                  courseId = CourseId(row[QuizTable.courseId]),
                  teacherId = TeacherId(row[QuizTable.teacherId]),
                  questionText = row[QuizTable.questionText],
                  answers = row[QuizTable.answers],
                  correctAnswerIndex = row[QuizTable.correctAnswerIndex],
                  createdAt = row[QuizTable.createdAt],
                  duration = row[QuizTable.duration].milliseconds,
                  activationTime = row[QuizTable.activationTime],
                )
              RichQuiz(
                id = quizId,
                metaInformation = metaInformation,
                isActive = row[QuizTable.isActive],
                studentAnswers = studentAnswers,
              )
            }
        Ok(quizzes)
      }
      .mapBoth(success = { it }, failure = { Err(it.asEduPlatformError(QuizRepository::class)) })

  fun findAllQuizzes(): EduPlatformResult<List<RichQuiz>> =
    runCatching {
        val quizzes =
          QuizTable.selectAll().map { row ->
            val quizId = QuizId(row[QuizTable.id].value)
            val studentAnswers =
              StudentAnswersTable.selectAll()
                .where { StudentAnswersTable.quizId eq quizId.long }
                .associate {
                  StudentId(it[StudentAnswersTable.studentId]) to
                    it[StudentAnswersTable.chosenAnswerIndex]
                }

            val metaInformation =
              QuizMetaInformation(
                courseId = CourseId(row[QuizTable.courseId]),
                teacherId = TeacherId(row[QuizTable.teacherId]),
                questionText = row[QuizTable.questionText],
                answers = row[QuizTable.answers],
                correctAnswerIndex = row[QuizTable.correctAnswerIndex],
                createdAt = row[QuizTable.createdAt],
                duration = row[QuizTable.duration].milliseconds,
                activationTime = row[QuizTable.activationTime],
              )
            RichQuiz(
              id = quizId,
              metaInformation = metaInformation,
              isActive = row[QuizTable.isActive],
              studentAnswers = studentAnswers,
            )
          }
        Ok(quizzes)
      }
      .mapBoth(success = { it }, failure = { Err(it.asEduPlatformError(QuizRepository::class)) })

  fun findQuizById(quizId: QuizId): EduPlatformResult<RichQuiz?> =
    runCatching {
        val row = QuizTable.selectAll().where { QuizTable.id eq quizId.long }.singleOrNull()
        row
          ?.let { singleQuizRow ->
            val studentAnswers =
              StudentAnswersTable.selectAll()
                .where { StudentAnswersTable.quizId eq quizId.long }
                .associate { studentAnswerRow ->
                  StudentId(studentAnswerRow[StudentAnswersTable.studentId]) to
                    studentAnswerRow[StudentAnswersTable.chosenAnswerIndex]
                }

            RichQuiz(
              id = QuizId(singleQuizRow[QuizTable.id].value),
              metaInformation =
                QuizMetaInformation(
                  courseId = CourseId(singleQuizRow[QuizTable.courseId]),
                  teacherId = TeacherId(singleQuizRow[QuizTable.teacherId]),
                  questionText = singleQuizRow[QuizTable.questionText],
                  answers = singleQuizRow[QuizTable.answers],
                  correctAnswerIndex = singleQuizRow[QuizTable.correctAnswerIndex],
                  createdAt = singleQuizRow[QuizTable.createdAt],
                  duration = singleQuizRow[QuizTable.duration].milliseconds,
                  activationTime = singleQuizRow[QuizTable.activationTime],
                ),
              isActive = singleQuizRow[QuizTable.isActive],
              studentAnswers = studentAnswers,
            )
          }
          .let { Ok(it) }
      }
      .mapBoth(success = { it }, failure = { Err(it.asEduPlatformError(QuizRepository::class)) })

  fun saveQuiz(quizMetaInformation: QuizMetaInformation): EduPlatformResult<QuizId> =
    runCatching {
        val id =
          QuizTable.insert {
              it[courseId] = quizMetaInformation.courseId.long
              it[teacherId] = quizMetaInformation.teacherId.long
              it[questionText] = quizMetaInformation.questionText
              it[answers] = quizMetaInformation.answers
              it[correctAnswerIndex] = quizMetaInformation.correctAnswerIndex
              it[createdAt] = quizMetaInformation.createdAt
              it[duration] = quizMetaInformation.duration.inWholeMilliseconds
              it[activationTime] = quizMetaInformation.activationTime
              it[isActive] = false
            }[QuizTable.id]
            .value
        Ok(QuizId(id))
      }
      .mapBoth(success = { it }, failure = { Err(it.asEduPlatformError(QuizRepository::class)) })

  /** `[activationTime] == null` means not to set activation time */
  fun updateQuizActivationStatus(
    quizId: QuizId,
    activationTime: Instant?,
    isActive: Boolean,
  ): EduPlatformResult<Unit> =
    runCatching {
        val updatedRows =
          QuizTable.update({ QuizTable.id eq quizId.long }) {
            if (activationTime != null) {
              it[QuizTable.activationTime] = activationTime
            }
            it[QuizTable.isActive] = isActive
          }
        if (updatedRows > 0) {
          Ok(Unit)
        } else {
          Err(NamedError("Quiz with ID ${quizId.long} not found.", QuizRepository::class))
        }
      }
      .mapBoth(success = { it }, failure = { Err(it.asEduPlatformError(QuizRepository::class)) })

  fun storeStudentAnswer(
    quizId: QuizId,
    studentId: StudentId,
    chosenAnswerIndex: Int,
  ): EduPlatformResult<Unit> =
    runCatching {
        val updatedRows =
          StudentAnswersTable.update({
            (StudentAnswersTable.quizId eq quizId.long) and
              (StudentAnswersTable.studentId eq studentId.long)
          }) {
            it[StudentAnswersTable.chosenAnswerIndex] = chosenAnswerIndex
          }
        if (updatedRows == 0) {
          StudentAnswersTable.insert {
            it[StudentAnswersTable.quizId] = quizId.long
            it[StudentAnswersTable.studentId] = studentId.long
            it[StudentAnswersTable.chosenAnswerIndex] = chosenAnswerIndex
          }
        }
        Ok(Unit)
      }
      .mapBoth(success = { it }, failure = { Err(it.asEduPlatformError(QuizRepository::class)) })

  fun getLastStudentAnswer(
    quizId: QuizId,
    studentId: StudentId,
  ): EduPlatformResult<StudentAnswer?> =
    runCatching {
        val result =
          StudentAnswersTable.selectAll()
            .where {
              (StudentAnswersTable.quizId eq quizId.long) and
                (StudentAnswersTable.studentId eq studentId.long)
            }
            .singleOrNull()

        val studentAnswer =
          result?.let {
            StudentAnswer(
              quizId = QuizId(it[StudentAnswersTable.quizId]),
              studentId = StudentId(it[StudentAnswersTable.studentId]),
              chosenAnswerIndex = it[StudentAnswersTable.chosenAnswerIndex],
            )
          }
        Ok(studentAnswer)
      }
      .mapBoth(success = { it }, failure = { Err(it.asEduPlatformError(QuizRepository::class)) })
}
