package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.SolutionTable
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class DatabaseSolutionDistributor(
  val database: Database,
) : SolutionDistributor {
  init {
    transaction(database) {
      SchemaUtils.create(SolutionTable)
    }
  }

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
        } get SolutionTable.id
      }.value
    return SolutionId(solutionId)
  }

  override fun querySolution(
    teacherId: TeacherId,
    gradeTable: GradeTable,
  ): SolutionId? = transaction(database) {
    SolutionTable
      .selectAll()
      .map { it[SolutionTable.id].value.toSolutionId() }
  }.firstOrNull { solutionId -> !gradeTable.isChecked(solutionId) }

  override fun resolveSolution(solutionId: SolutionId): Solution =
    transaction(database) {
      val solution =
        SolutionTable
          .selectAll()
          .where { SolutionTable.id eq solutionId.id }
          .first()

      return@transaction Solution(
        solutionId,
        StudentId(solution[SolutionTable.studentId].value),
        solution[SolutionTable.chatId].toChatId().chatId,
        MessageId(solution[SolutionTable.messageId]),
        ProblemId(solution[SolutionTable.problemId].value),
        SolutionContent(listOf(), solution[SolutionTable.content]),
        SolutionType.TEXT,
      )
    }
}
