package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionDistributor
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
import kotlin.math.absoluteValue

class DatabaseSolutionDistributor(
  val database: Database,
) : SolutionDistributor {
  override fun inputSolution(
    studentId: String,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
  ) {
    val solutionId =
      transaction(database) {
        SolutionTable.insert {
          it[SolutionTable.studentId] = studentId.toIntIdHack()
          it[SolutionTable.chatId] = chatId.toChatId().chatId.long
          it[SolutionTable.messageId] = messageId.long
          it[SolutionTable.problemId] = 0 // TODO: Add this to the method arguments
          it[SolutionTable.content] = solutionContent.text
        } get SolutionTable.id
      }.value.toString()
  }

  override fun querySolution(teacherId: String): Solution? {
    val solutions =
      transaction(database) {
        val assessedSolutions = AssessmentTable.select(AssessmentTable.id).withDistinctOn(AssessmentTable.id)

        SolutionTable
          .selectAll()
          .where { SolutionTable.id notInSubQuery assessedSolutions }
          .orderBy(SolutionTable.timestamp)
      }

    val solution = solutions.firstOrNull() ?: return null
    return Solution(
      solution[SolutionTable.id].value.toString(),
      solution[SolutionTable.studentId].value.toString(),
      RawChatId(solution[SolutionTable.chatId].absoluteValue),
      MessageId(solution[SolutionTable.messageId]),
      Problem(solution[SolutionTable.problemId].value.toString(), "", "", 1, ""),
      SolutionContent(listOf(), solution[SolutionTable.content]),
      SolutionType.TEXT,
    )
  }

  override fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: LocalDateTime,
    teacherStatistics: TeacherStatistics,
  ) {
    transaction(database) {
      AssessmentTable.insert {
        it[AssessmentTable.solutionId] = solution.id.toIntIdHack()
        it[AssessmentTable.teacherId] = teacherId.toIntIdHack()
        it[AssessmentTable.grade] = assessment.grade
      }
    }
  }
}
