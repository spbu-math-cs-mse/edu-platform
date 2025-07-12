package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.CourseTokenService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ScheduledMessageService
import com.github.heheteam.commonlib.logic.StudentViewService
import com.github.heheteam.commonlib.logic.SubmissionSendingResult
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.UserId
import kotlinx.datetime.LocalDateTime

@Suppress("LongParameterList", "TooManyFunctions")
class StudentApi
internal constructor(
  private val academicWorkflowService: AcademicWorkflowService,
  private val personalDeadlinesService: PersonalDeadlinesService,
  private val scheduledMessageDeliveryService: ScheduledMessageService,
  private val studentViewService: StudentViewService,
  private val studentStorage: StudentStorage,
  private val courseTokenService: CourseTokenService,
  private val errorManagementService: ErrorManagementService,
) {
  suspend fun checkAndSendMessages(timestamp: LocalDateTime): Result<Unit, NumberedError> =
    errorManagementService.coroutineServiceBinding {
      scheduledMessageDeliveryService.checkAndSendMessages(timestamp)
    }

  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): Result<List<Pair<Problem, ProblemGrade>>, NumberedError> =
    errorManagementService.serviceBinding {
      academicWorkflowService.getGradingsForAssignment(assignmentId, studentId).bind()
    }

  fun getAllCourses(): Result<List<Course>, NumberedError> =
    errorManagementService.serviceBinding { studentViewService.getAllCourses().bind() }

  fun getStudentCourses(studentId: StudentId): Result<List<Course>, NumberedError> =
    errorManagementService.serviceBinding { studentViewService.getStudentCourses(studentId).bind() }

  fun getCourseAssignments(courseId: CourseId): Result<List<Assignment>, NumberedError> =
    errorManagementService.serviceBinding {
      studentViewService.getCourseAssignments(courseId).bind()
    }

  suspend fun inputSubmission(
    submissionInputRequest: SubmissionInputRequest
  ): SubmissionSendingResult = academicWorkflowService.sendSubmission(submissionInputRequest)

  fun getProblemsFromAssignment(assignmentId: AssignmentId): Result<List<Problem>, NumberedError> =
    errorManagementService.serviceBinding {
      studentViewService.getProblemsFromAssignment(assignmentId).bind()
    }

  fun loginByTgId(tgId: UserId): Result<Student?, NumberedError> =
    errorManagementService.serviceBinding { studentStorage.resolveByTgId(tgId).bind() }

  fun loginById(studentId: StudentId): Result<Student?, NumberedError> =
    errorManagementService.serviceBinding { studentStorage.resolveStudent(studentId).bind() }

  fun updateTgId(studentId: StudentId, newTgId: UserId): Result<Unit, NumberedError> =
    errorManagementService.serviceBinding { studentStorage.updateTgId(studentId, newTgId).bind() }

  fun createStudent(name: String, surname: String, tgId: Long): Result<StudentId, NumberedError> =
    errorManagementService.serviceBinding {
      studentStorage.createStudent(name, surname, tgId).bind()
    }

  suspend fun requestReschedulingDeadlines(
    studentId: StudentId,
    newDeadline: LocalDateTime,
  ): Result<Unit, NumberedError> =
    errorManagementService.coroutineServiceBinding {
      personalDeadlinesService.requestReschedulingDeadlines(studentId, newDeadline).bind()
    }

  fun calculateRescheduledDeadlines(studentId: StudentId, problems: List<Problem>): List<Problem> =
    personalDeadlinesService.calculateNewDeadlines(studentId, problems)

  fun getActiveProblems(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Map<Assignment, List<Problem>>, NumberedError> =
    errorManagementService.serviceBinding {
      personalDeadlinesService.getActiveProblems(studentId, courseId).bind()
    }

  fun registerForCourseWithToken(
    token: String,
    studentId: StudentId,
  ): Result<Course, NumberedError> =
    errorManagementService.serviceBinding {
      courseTokenService.registerStudentForToken(studentId, token).bind()
    }
}
