package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.tables.AssessmentTable
import com.github.heheteam.commonlib.database.tables.SolutionTable
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class DatabaseSolutionDistributor(
  val database: Database,
) : SolutionDistributor {
  override fun inputSolution(
    studentId: Long,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
  ): SolutionId {
    val solutionId =
      transaction(database) {
        SolutionTable.insert {
          it[SolutionTable.studentId] = studentId
          it[SolutionTable.chatId] = chatId.toChatId().chatId.long
          it[SolutionTable.messageId] = messageId.long
          it[SolutionTable.problemId] = problemId
          it[SolutionTable.content] = solutionContent.text
        } get SolutionTable.id
      }.value
    return solutionId
  }

  override fun querySolution(teacherId: Long): SolutionId? {
    val solutions =
      transaction(database) {
        val assessedSolutions = AssessmentTable.select(AssessmentTable.id)
          .withDistinctOn(AssessmentTable.id)

        SolutionTable
          .selectAll()
          .where { SolutionTable.id notInSubQuery assessedSolutions }
          .orderBy(SolutionTable.timestamp)
      }

    val solution = solutions.firstOrNull() ?: return null
    TODO("Not implemented")
  }

  override fun resolveSolution(solutionId: SolutionId): Solution {
    TODO("Not yet implemented")
  }

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: LocalDateTime,
    teacherStatistics: TeacherStatistics,
  ) {
    transaction(database) {
      AssessmentTable.insert {
        it[AssessmentTable.solutionId] = solutionId
        it[AssessmentTable.teacherId] = teacherId
        it[AssessmentTable.grade] = assessment.grade
      }
    }
  }
}
