package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.SubmissionResolveError
import com.github.heheteam.commonlib.TeacherDoesNotExist
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SubmissionTable
import com.github.heheteam.commonlib.database.table.TeacherTable
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toSubmissionId
import com.github.heheteam.commonlib.interfaces.toTeacherId
import com.github.heheteam.commonlib.util.catchingTransaction
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseSubmissionDistributor(val database: Database) : SubmissionDistributor {
  override fun inputSubmission(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    submissionContent: TextWithMediaAttachments,
    problemId: ProblemId,
    timestamp: LocalDateTime,
    teacherId: TeacherId?,
  ): SubmissionId {
    val submissionId =
      transaction(database) {
          SubmissionTable.insert {
            it[SubmissionTable.studentId] = studentId.long
            it[SubmissionTable.chatId] = chatId.toChatId().chatId.long
            it[SubmissionTable.messageId] = messageId.long
            it[SubmissionTable.problemId] = problemId.long
            it[SubmissionTable.timestamp] = timestamp.toKotlinLocalDateTime()
            it[SubmissionTable.submissionContent] = submissionContent
            it[SubmissionTable.responsibleTeacher] = teacherId?.long
          } get SubmissionTable.id
        }
        .value
    return SubmissionId(submissionId)
  }

  @Suppress("LongMethod") // ok, as it is a just a long query
  override fun querySubmission(teacherId: TeacherId): Result<Submission?, SubmissionResolveError> =
    transaction(database) {
      val teacherRow =
        TeacherTable.select(TeacherTable.id).where(TeacherTable.id eq teacherId.long).firstOrNull()
          ?: return@transaction Err(TeacherDoesNotExist(teacherId))
      val courses =
        CourseTeachers.select(CourseTeachers.courseId)
          .where(CourseTeachers.teacherId eq teacherRow[TeacherTable.id])
          .map { course -> course[CourseTeachers.courseId] }

      val submission =
        SubmissionTable.join(
            AssessmentTable,
            JoinType.LEFT,
            onColumn = SubmissionTable.id,
            otherColumn = AssessmentTable.submissionId,
          )
          .join(
            ProblemTable,
            JoinType.INNER,
            onColumn = SubmissionTable.problemId,
            otherColumn = ProblemTable.id,
          )
          .join(
            AssignmentTable,
            JoinType.INNER,
            onColumn = ProblemTable.assignmentId,
            otherColumn = AssignmentTable.id,
          )
          .join(
            CourseTable,
            JoinType.INNER,
            onColumn = AssignmentTable.courseId,
            otherColumn = CourseTable.id,
          )
          .selectAll()
          .where {
            AssessmentTable.id.isNull() and
              (CourseTable.id inList courses) and
              (SubmissionTable.responsibleTeacher eq teacherId.long)
          }
          .orderBy(SubmissionTable.timestamp)
          .firstOrNull() ?: return@transaction Ok(null)

      Ok(
        Submission(
          submission[SubmissionTable.id].value.toSubmissionId(),
          StudentId(submission[SubmissionTable.studentId].value),
          submission[SubmissionTable.chatId].toChatId().chatId,
          MessageId(submission[SubmissionTable.messageId]),
          ProblemId(submission[SubmissionTable.problemId].value),
          submission[SubmissionTable.submissionContent],
          submission[SubmissionTable.responsibleTeacher]?.value?.toTeacherId(),
          submission[SubmissionTable.timestamp],
        )
      )
    }

  @Suppress("LongMethod") // a long database query
  override fun querySubmission(courseId: CourseId): Result<Submission?, SubmissionResolveError> =
    transaction(database) {
      val submission =
        SubmissionTable.join(
            AssessmentTable,
            JoinType.LEFT,
            onColumn = SubmissionTable.id,
            otherColumn = AssessmentTable.submissionId,
          )
          .join(
            ProblemTable,
            JoinType.INNER,
            onColumn = SubmissionTable.problemId,
            otherColumn = ProblemTable.id,
          )
          .join(
            AssignmentTable,
            JoinType.INNER,
            onColumn = ProblemTable.assignmentId,
            otherColumn = AssignmentTable.id,
          )
          .join(
            CourseTable,
            JoinType.INNER,
            onColumn = AssignmentTable.courseId,
            otherColumn = CourseTable.id,
          )
          .selectAll()
          .where { AssessmentTable.id.isNull() and (CourseTable.id eq courseId.long) }
          .orderBy(SubmissionTable.timestamp)
          .firstOrNull() ?: return@transaction Ok(null)

      Ok(
        Submission(
          submission[SubmissionTable.id].value.toSubmissionId(),
          StudentId(submission[SubmissionTable.studentId].value),
          submission[SubmissionTable.chatId].toChatId().chatId,
          MessageId(submission[SubmissionTable.messageId]),
          ProblemId(submission[SubmissionTable.problemId].value),
          submission[SubmissionTable.submissionContent],
          submission[SubmissionTable.responsibleTeacher]?.value?.toTeacherId(),
          submission[SubmissionTable.timestamp],
        )
      )
    }

  override fun resolveSubmission(
    submissionId: SubmissionId
  ): Result<Submission, ResolveError<SubmissionId>> =
    transaction(database) {
      val submission =
        SubmissionTable.selectAll().where { SubmissionTable.id eq submissionId.long }.singleOrNull()
          ?: return@transaction Err(ResolveError(submissionId))

      Ok(
        Submission(
          submissionId,
          StudentId(submission[SubmissionTable.studentId].value),
          submission[SubmissionTable.chatId].toChatId().chatId,
          MessageId(submission[SubmissionTable.messageId]),
          ProblemId(submission[SubmissionTable.problemId].value),
          submission[SubmissionTable.submissionContent],
          submission[SubmissionTable.responsibleTeacher]?.value?.toTeacherId(),
          submission[SubmissionTable.timestamp],
        )
      )
    }

  override fun resolveSubmissionCourse(
    submissionId: SubmissionId
  ): Result<CourseId, ResolveError<SubmissionId>> =
    transaction(database) {
      SubmissionTable.join(
          ProblemTable,
          JoinType.INNER,
          onColumn = SubmissionTable.problemId,
          otherColumn = ProblemTable.id,
        )
        .join(
          AssignmentTable,
          JoinType.INNER,
          onColumn = ProblemTable.assignmentId,
          otherColumn = AssignmentTable.id,
        )
        .selectAll()
        .where { SubmissionTable.id eq submissionId.long }
        .singleOrNull()
        ?.let { Ok(CourseId(it[AssignmentTable.courseId].value)) }
        ?: return@transaction Err(ResolveError(submissionId))
    }

  override fun resolveResponsibleTeacher(
    submissionInputRequest: SubmissionInputRequest
  ): TeacherId? =
    transaction(database) {
      SubmissionTable.selectAll()
        .where {
          (SubmissionTable.problemId eq submissionInputRequest.problemId.long) and
            (SubmissionTable.studentId eq submissionInputRequest.studentId.long)
        }
        .map { row -> row[SubmissionTable.responsibleTeacher]?.value?.toTeacherId() }
        .firstOrNull()
    }

  override fun getSubmissionsForProblem(
    problemId: ProblemId
  ): Result<List<SubmissionId>, EduPlatformError> =
    catchingTransaction(database) {
      SubmissionTable.selectAll()
        .where { SubmissionTable.problemId eq problemId.long }
        .map { row -> SubmissionId(row[SubmissionTable.id].value) }
    }

  override fun isSubmissionAssessed(submissionId: SubmissionId): Result<Boolean, EduPlatformError> =
    catchingTransaction(database) {
      AssessmentTable.select(AssessmentTable.id)
        .where { AssessmentTable.submissionId eq submissionId.long }
        .any()
    }
}
