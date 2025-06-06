package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.CourseTokenStorage
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TokenError
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ScheduledMessageDeliveryService
import com.github.heheteam.commonlib.logic.SubmissionSendingResult
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.types.UserId
import kotlinx.datetime.LocalDateTime

@Suppress("LongParameterList", "TooManyFunctions")
class StudentApi
internal constructor(
  private val courseStorage: CourseStorage,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val academicWorkflowService: AcademicWorkflowService,
  private val personalDeadlinesService: PersonalDeadlinesService,
  private val studentStorage: StudentStorage,
  private val courseTokenStorage: CourseTokenStorage,
  private val scheduledMessageDeliveryService: ScheduledMessageDeliveryService, // New dependency
) {
  fun checkAndSentMessages(timestamp: LocalDateTime): Result<Unit, EduPlatformError> =
    scheduledMessageDeliveryService.checkAndSendMessages(timestamp)

  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, ProblemGrade>> =
    academicWorkflowService.getGradingsForAssignment(assignmentId, studentId)

  fun getAllCourses(): List<Course> = courseStorage.getCourses()

  fun getStudentCourses(studentId: StudentId): List<Course> =
    courseStorage.getStudentCourses(studentId)

  fun getCourseAssignments(courseId: CourseId): Result<List<Assignment>, EduPlatformError> =
    assignmentStorage.getAssignmentsForCourse(courseId)

  fun applyForCourse(studentId: StudentId, courseId: CourseId) =
    courseStorage.addStudentToCourse(studentId, courseId)

  fun inputSubmission(submissionInputRequest: SubmissionInputRequest): SubmissionSendingResult =
    academicWorkflowService.sendSubmission(submissionInputRequest)

  fun getProblemsFromAssignment(assignmentId: AssignmentId): List<Problem> =
    problemStorage.getProblemsFromAssignment(assignmentId)

  fun loginByTgId(tgId: UserId): Result<Student, ResolveError<UserId>> =
    studentStorage.resolveByTgId(tgId)

  fun loginById(studentId: StudentId): Result<Student, ResolveError<StudentId>> =
    studentStorage.resolveStudent(studentId)

  fun updateTgId(studentId: StudentId, newTgId: UserId): Result<Unit, ResolveError<StudentId>> =
    studentStorage.updateTgId(studentId, newTgId)

  fun createStudent(name: String, surname: String, tgId: Long): StudentId =
    studentStorage.createStudent(name, surname, tgId)

  /**
   * Returns assignments and all of their problems with original deadlines. You might need to
   * calculate personal deadlines additionally.
   */
  fun getProblemsWithAssignmentsFromCourse(courseId: CourseId): Map<Assignment, List<Problem>> =
    problemStorage.getProblemsWithAssignmentsFromCourse(courseId)

  fun requestReschedulingDeadlines(studentId: StudentId, newDeadline: LocalDateTime) =
    personalDeadlinesService.requestReschedulingDeadlines(studentId, newDeadline)

  fun calculateRescheduledDeadlines(studentId: StudentId, problems: List<Problem>): List<Problem> =
    personalDeadlinesService.calculateNewDeadlines(studentId, problems)

  fun calculateRescheduledDeadlines(
    studentId: StudentId,
    problems: Map<Assignment, List<Problem>>,
  ): Map<Assignment, List<Problem>> =
    personalDeadlinesService.calculateNewDeadlines(studentId, problems)

  fun registerForCourseWithToken(token: String, studentId: StudentId): Result<Course, TokenError> {
    val courseIdResult = courseTokenStorage.getCourseIdByToken(token)

    return courseIdResult.mapBoth(
      success = { courseId ->
        courseStorage.addStudentToCourse(studentId, courseId)
        courseTokenStorage.useToken(token, studentId)
        val course = getStudentCourses(studentId).first { it.id == courseId }
        Ok(course)
      },
      failure = { error -> Err(error) },
    )
  }
}
