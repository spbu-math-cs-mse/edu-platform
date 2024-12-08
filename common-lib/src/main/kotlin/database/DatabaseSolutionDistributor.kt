package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.*
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class DatabaseSolutionDistributor(
  val database: Database,
) : SolutionDistributor {
  override fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
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
          it[SolutionTable.timestamp] = timestamp.toKotlinLocalDateTime()
        } get SolutionTable.id
      }.value
    return SolutionId(solutionId)
  }

  override fun querySolution(
    teacherId: TeacherId,
    gradeTable: GradeTable,
  ): Solution? =
    transaction(database) {
      val id =
        TeacherTable.select(TeacherTable.id)
          .where(TeacherTable.tgId eq teacherId.id)
          .firstOrNull() ?: return@transaction null
      val courses =
        CourseTeachers.select(CourseTeachers.courseId)
          .where(CourseTeachers.teacherId eq id[TeacherTable.id])
          .toList()
      val assignments =
        AssignmentTable
          .select(AssignmentTable.id)
          .where(
            AssignmentTable.course inList
              courses.map { course -> course[CourseTeachers.courseId] },
          )
          .toList()
      val problems =
        ProblemTable
          .select(ProblemTable.id)
          .where(
            ProblemTable.assignmentId inList
              assignments.map { assignment -> assignment[AssignmentTable.id] },
          )

      val solution =
        SolutionTable
          .join(AssessmentTable, JoinType.LEFT, onColumn = SolutionTable.id, otherColumn = AssessmentTable.solutionId)
          .selectAll()
          .where {
            AssessmentTable.id.isNull() and (
              SolutionTable.problemId inList
                problems.map { problem -> problem[ProblemTable.id] }
              )
          }
          .firstOrNull() ?: return@transaction null

      Solution(
        solution[SolutionTable.id].value.toSolutionId(),
        StudentId(solution[SolutionTable.studentId].value),
        solution[SolutionTable.chatId].toChatId().chatId,
        MessageId(solution[SolutionTable.messageId]),
        ProblemId(solution[SolutionTable.problemId].value),
        SolutionContent(listOf(), solution[SolutionTable.content]),
        SolutionType.TEXT,
        solution[SolutionTable.timestamp].toJavaLocalDateTime(),
      )
    }

  override fun resolveSolution(solutionId: SolutionId): Result<Solution, ResolveError<SolutionId>> =
    transaction(database) {
      val solution =
        SolutionTable
          .selectAll()
          .where { SolutionTable.id eq solutionId.id }
          .singleOrNull() ?: return@transaction Err(ResolveError(solutionId))

      Ok(
        Solution(
          solutionId,
          StudentId(solution[SolutionTable.studentId].value),
          solution[SolutionTable.chatId].toChatId().chatId,
          MessageId(solution[SolutionTable.messageId]),
          ProblemId(solution[SolutionTable.problemId].value),
          SolutionContent(listOf(), solution[SolutionTable.content]),
          SolutionType.TEXT,
          solution[SolutionTable.timestamp].toJavaLocalDateTime(),
        ),
      )
    }
}
