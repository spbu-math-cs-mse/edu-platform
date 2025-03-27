package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

class AcademicWorkflowLogic(
  private val solutionDistributor: SolutionDistributor,
  private val gradeTable: GradeTable,
  private val problemStorage: ProblemStorage,
) {
  fun inputSolution(
    solutionInputRequest: SolutionInputRequest,
    responsibleTeacher: TeacherId,
  ): SolutionId {
    return solutionDistributor.inputSolution(
      solutionInputRequest.studentId,
      solutionInputRequest.telegramMessageInfo.chatId,
      solutionInputRequest.telegramMessageInfo.messageId,
      solutionInputRequest.solutionContent,
      solutionInputRequest.problemId,
      solutionInputRequest.timestamp.toJavaLocalDateTime(),
      responsibleTeacher,
    )
  }

  fun assessSolution(
    solutionId: SolutionId,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: LocalDateTime,
  ) {
    gradeTable.recordSolutionAssessment(solutionId, teacherId, assessment, timestamp)
  }

  fun getGradingsForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): Pair<List<Problem>, Map<ProblemId, Grade?>> {
    val problems =
      problemStorage.getProblemsFromAssignment(assignmentId).sortedBy { problem ->
        problem.serialNumber
      }
    val grades = gradeTable.getStudentPerformance(studentId, listOf(assignmentId))
    return problems to grades
  }

  fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>> =
    gradeTable.getCourseRating(courseId)
}
