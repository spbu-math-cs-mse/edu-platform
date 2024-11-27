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
  private var solutionId = 1L

  override fun inputSolution(
    studentId: Long,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
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
        0L,
        solutionContent,
        solutionType,
      )
    solutions.add(solution)
    return solution.id
  }

  override fun querySolution(teacherId: Long): SolutionId? =
    solutions.firstOrNull()?.id

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
    solutions.removeIf { it.id == solutionId }
//    teacherStatistics.recordAssessment(teacherId, solutionId, timestamp)
    gradeTable.addAssessment(teacherId, solutionId, assessment)
  }
}
