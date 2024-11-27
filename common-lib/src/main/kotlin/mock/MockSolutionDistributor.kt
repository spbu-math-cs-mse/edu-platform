package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlin.collections.mutableListOf

class MockSolutionDistributor() : SolutionDistributor {
  private val solutions = mutableListOf<Solution>()
  private var solutionId = 1L

  override fun inputSolution(
    studentId: Long,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
  ) {
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
        Problem(0L, "", "", 0, 0L),
        solutionContent,
        solutionType,
      )
    solutions.add(solution)
  }

  override fun querySolution(teacherId: Long): Solution? {
    if (solutions.isEmpty()) {
      return null
    }
    return solutions.first()
  }

  override fun assessSolution(
    solution: Solution,
    teacherId: Long,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: LocalDateTime,
    teacherStatistics: TeacherStatistics,
  ) {
    solutions.removeIf { it == solution }
    teacherStatistics.recordAssessment(teacherId, solution, timestamp)
    gradeTable.addAssessment(Student(solution.studentId), Teacher(teacherId), solution, assessment)
  }
}
