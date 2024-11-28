package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlin.collections.mutableListOf

class InMemorySolutionDistributor() : SolutionDistributor {
  private val solutions = mutableListOf<Solution>()
  private val assessedSolutions = mutableListOf<SolutionId>()
  private var solutionId = 1L

  override fun inputSolution(
    studentId: Long,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
    timestamp: LocalDateTime,
  ): SolutionId {
    val solutionType =
      when (solutionContent.text) {
        SolutionType.PHOTOS.toString() -> SolutionType.PHOTOS
        SolutionType.PHOTO.toString() -> SolutionType.PHOTO
        SolutionType.DOCUMENT.toString() -> SolutionType.DOCUMENT
        else -> SolutionType.TEXT
      }
    val solution =
      Solution(
        solutionId++,
        studentId,
        chatId,
        messageId,
        problemId,
        solutionContent,
        solutionType,
        timestamp,
      )
    solutions.add(solution)
    return solution.id
  }

  override fun querySolution(teacherId: Long): SolutionId? =
    solutions.filter { !assessedSolutions.contains(it.id) }.firstOrNull()?.id

  override fun resolveSolution(solutionId: SolutionId): Solution {
    return solutions.single { it.id == solutionId }
  }

  override fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    teacherStatistics: TeacherStatistics,
    timestamp: LocalDateTime,
  ) {
    teacherStatistics.recordAssessment(teacherId, solutionId, timestamp, this)
    gradeTable.addAssessment(teacherId, solutionId, assessment)
    assessedSolutions.add(solutionId)
  }
}
