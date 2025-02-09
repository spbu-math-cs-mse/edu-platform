package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.TelegramAttachment
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ResolveError
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.SolutionResolveError
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherDoesNotExist
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.toSolutionId
import com.github.heheteam.commonlib.database.table.AssessmentTable
import com.github.heheteam.commonlib.database.table.AssignmentTable
import com.github.heheteam.commonlib.database.table.CourseTable
import com.github.heheteam.commonlib.database.table.CourseTeachers
import com.github.heheteam.commonlib.database.table.ProblemTable
import com.github.heheteam.commonlib.database.table.SolutionTable
import com.github.heheteam.commonlib.database.table.TeacherTable
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

class DatabaseSolutionDistributor(val database: Database) : SolutionDistributor {
  override fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    attachment: TelegramAttachment,
    problemId: ProblemId,
    timestamp: LocalDateTime,
  ): SolutionId {
    val solutionId =
      transaction(database) {
          SolutionTable.insert {
            it[SolutionTable.studentId] = studentId.id
            it[SolutionTable.chatId] = chatId.toChatId().chatId.long
            it[SolutionTable.messageId] = messageId.long
            it[SolutionTable.problemId] = problemId.id
            it[SolutionTable.content] = solutionContent.text ?: ""
            it[SolutionTable.fileUrl] = solutionContent.filesURL ?: listOf()
            it[SolutionTable.solutionType] = solutionContent.type.toString()
            it[SolutionTable.timestamp] = timestamp.toKotlinLocalDateTime()
            it[SolutionTable.attachments] = attachment
          } get SolutionTable.id
        }
        .value
    return SolutionId(solutionId)
  }

  override fun querySolution(teacherId: TeacherId): Result<Solution?, SolutionResolveError> =
    transaction(database) {
      val teacherRow =
        TeacherTable.select(TeacherTable.id).where(TeacherTable.id eq teacherId.id).firstOrNull()
          ?: return@transaction Err(TeacherDoesNotExist(teacherId))
      val courses =
        CourseTeachers.select(CourseTeachers.courseId)
          .where(CourseTeachers.teacherId eq teacherRow[TeacherTable.id])
          .map { course -> course[CourseTeachers.courseId] }

      val solution =
        SolutionTable.join(
            AssessmentTable,
            JoinType.LEFT,
            onColumn = SolutionTable.id,
            otherColumn = AssessmentTable.solutionId,
          )
          .join(
            ProblemTable,
            JoinType.INNER,
            onColumn = SolutionTable.problemId,
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
          .where { AssessmentTable.id.isNull() and (CourseTable.id inList courses) }
          .orderBy(SolutionTable.timestamp)
          .firstOrNull() ?: return@transaction Ok(null)

      Ok(
        Solution(
          solution[SolutionTable.id].value.toSolutionId(),
          StudentId(solution[SolutionTable.studentId].value),
          solution[SolutionTable.chatId].toChatId().chatId,
          MessageId(solution[SolutionTable.messageId]),
          ProblemId(solution[SolutionTable.problemId].value),
          SolutionContent(
            solution[SolutionTable.fileUrl],
            solution[SolutionTable.content],
            SolutionType.valueOf(solution[SolutionTable.solutionType]),
          ),
          solution[SolutionTable.attachments],
          solution[SolutionTable.timestamp],
        )
      )
    }

  override fun resolveSolution(solutionId: SolutionId): Result<Solution, ResolveError<SolutionId>> =
    transaction(database) {
      val solution =
        SolutionTable.selectAll().where { SolutionTable.id eq solutionId.id }.singleOrNull()
          ?: return@transaction Err(ResolveError(solutionId))

      Ok(
        Solution(
          solutionId,
          StudentId(solution[SolutionTable.studentId].value),
          solution[SolutionTable.chatId].toChatId().chatId,
          MessageId(solution[SolutionTable.messageId]),
          ProblemId(solution[SolutionTable.problemId].value),
          SolutionContent(
            solution[SolutionTable.fileUrl],
            solution[SolutionTable.content],
            SolutionType.valueOf(solution[SolutionTable.solutionType]),
          ),
          solution[SolutionTable.attachments],
          solution[SolutionTable.timestamp],
        )
      )
    }

  override fun getSolutionsForProblem(problemId: ProblemId): List<SolutionId> =
    transaction(database) {
      SolutionTable.selectAll()
        .where { SolutionTable.problemId eq problemId.id }
        .map { row -> SolutionId(row[SolutionTable.id].value) }
    }

  override fun isSolutionAssessed(solutionId: SolutionId): Boolean =
    transaction(database) {
      AssessmentTable.select(AssessmentTable.id)
        .where { AssessmentTable.solutionId eq solutionId.id }
        .firstOrNull() != null
    }
}
