package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.MockSolutionDistributor
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import javax.management.Query
import kotlin.collections.mutableListOf
import com.github.heheteam.commonlib.statistics.MockTeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatistics

class MockSolutionDistributor(
    private val teacherStatistics: TeacherStatistics = MockTeacherStatistics(),
) : SolutionDistributor {

  private val solutions = mutableListOf<Solution>()
  private var solutionId = 1

  override fun inputSolution(
    studentId: String,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
  ): Solution {
    val solutionType = when (solutionContent.text) {
      SolutionType.PHOTOS.toString() -> SolutionType.PHOTOS
      SolutionType.PHOTO.toString() -> SolutionType.PHOTO
      SolutionType.DOCUMENT.toString() -> SolutionType.DOCUMENT
      else -> SolutionType.TEXT
    }
    val solution = Solution(
      (solutionId++).toString(),
      studentId,
      chatId,
      messageId,
      Problem(""),
      solutionContent,
      solutionType
    )
    solutions.add(solution)
    return solution
  }

  override fun querySolution(teacherId: String): Solution? {
    if (solutions.isEmpty())
      return null
    return solutions.first()
  }

  override fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
  ) {
    solutions.removeIf{ it == solution }
    teacherStatistics.recordAssessment(teacherId)
    gradeTable.addAssessment(Student(solution.studentId), Teacher(teacherId), solution, assessment)
  }
}