package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssessmentTable
import com.github.heheteam.commonlib.database.tables.SolutionTable
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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
          it[SolutionTable.fileUrl] = solutionContent.filesURL ?: listOf()
          it[SolutionTable.solutionType] = solutionContent.type.toString()
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
      val solution = SolutionTable
        .join(AssessmentTable, JoinType.LEFT, onColumn = SolutionTable.id, otherColumn = AssessmentTable.solutionId)
        .selectAll()
        .where { AssessmentTable.id.isNull() }
        .firstOrNull() ?: return@transaction null

      Solution(
        solution[SolutionTable.id].value.toSolutionId(),
        StudentId(solution[SolutionTable.studentId].value),
        solution[SolutionTable.chatId].toChatId().chatId,
        MessageId(solution[SolutionTable.messageId]),
        ProblemId(solution[SolutionTable.problemId].value),
        SolutionContent(solution[SolutionTable.fileUrl], solution[SolutionTable.content]),
        SolutionType.valueOf(solution[SolutionTable.solutionType]),
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
          SolutionContent(solution[SolutionTable.fileUrl], solution[SolutionTable.content]),
          SolutionType.valueOf(solution[SolutionTable.solutionType]),
          solution[SolutionTable.timestamp].toJavaLocalDateTime(),
        ),
      )
    }
}
