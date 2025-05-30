package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

class AcademicWorkflowLogic
internal constructor(
  private val submissionDistributor: SubmissionDistributor,
  private val gradeTable: GradeTable,
) {
  fun inputSubmission(
    submissionInputRequest: SubmissionInputRequest,
    responsibleTeacher: TeacherId,
  ): SubmissionId {
    return submissionDistributor.inputSubmission(
      submissionInputRequest.studentId,
      submissionInputRequest.telegramMessageInfo.chatId,
      submissionInputRequest.telegramMessageInfo.messageId,
      submissionInputRequest.submissionContent,
      submissionInputRequest.problemId,
      submissionInputRequest.timestamp.toJavaLocalDateTime(),
      responsibleTeacher,
    )
  }

  fun assessSubmission(
    submissionId: SubmissionId,
    teacherId: TeacherId,
    assessment: SubmissionAssessment,
    timestamp: LocalDateTime,
  ) {
    gradeTable.recordSubmissionAssessment(submissionId, teacherId, assessment, timestamp)
  }

  fun getGradingsForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, ProblemGrade>> {
    return gradeTable.getStudentPerformance(studentId, assignmentId)
  }

  fun getCourseRating(courseId: CourseId): Map<StudentId, Map<ProblemId, Grade?>> =
    gradeTable.getCourseRating(courseId)
}
