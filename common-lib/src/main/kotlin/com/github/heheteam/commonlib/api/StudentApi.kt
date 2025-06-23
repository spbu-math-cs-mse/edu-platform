package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.SubmissionInputRequest
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
) {
  suspend fun checkAndSendMessages(timestamp: LocalDateTime): Result<Unit, EduPlatformError> =
    scheduledMessageDeliveryService.checkAndSendMessages(timestamp)

  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): Result<List<Pair<Problem, ProblemGrade>>, EduPlatformError> =
    academicWorkflowService.getGradingsForAssignment(assignmentId, studentId)

  fun getAllCourses(): Result<List<Course>, EduPlatformError> = studentViewService.getAllCourses()

  fun getStudentCourses(studentId: StudentId): Result<List<Course>, EduPlatformError> =
    studentViewService.getStudentCourses(studentId)

  fun getCourseAssignments(courseId: CourseId): Result<List<Assignment>, EduPlatformError> =
    studentViewService.getCourseAssignments(courseId)

  suspend fun inputSubmission(
    submissionInputRequest: SubmissionInputRequest
  ): SubmissionSendingResult = academicWorkflowService.sendSubmission(submissionInputRequest)

  fun getProblemsFromAssignment(
    assignmentId: AssignmentId
  ): Result<List<Problem>, EduPlatformError> =
    studentViewService.getProblemsFromAssignment(assignmentId)

  fun loginByTgId(tgId: UserId): Result<Student, ResolveError<UserId>> =
    studentStorage.resolveByTgId(tgId)

  fun loginById(studentId: StudentId): Result<Student, ResolveError<StudentId>> =
    studentStorage.resolveStudent(studentId)

  fun updateTgId(studentId: StudentId, newTgId: UserId): Result<Unit, ResolveError<StudentId>> =
    studentStorage.updateTgId(studentId, newTgId)

  fun createStudent(
    name: String,
    surname: String,
    tgId: Long,
  ): Result<StudentId, EduPlatformError> = studentStorage.createStudent(name, surname, tgId)

  suspend fun requestReschedulingDeadlines(studentId: StudentId, newDeadline: LocalDateTime) =
    personalDeadlinesService.requestReschedulingDeadlines(studentId, newDeadline)

  fun calculateRescheduledDeadlines(studentId: StudentId, problems: List<Problem>): List<Problem> =
    personalDeadlinesService.calculateNewDeadlines(studentId, problems)

  fun getActiveProblems(studentId: StudentId, courseId: CourseId) =
    personalDeadlinesService.getActiveProblems(studentId, courseId)

  fun registerForCourseWithToken(
    token: String,
    studentId: StudentId,
  ): Result<Course, EduPlatformError> = courseTokenService.registerStudentForToken(studentId, token)
}
