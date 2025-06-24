package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.CourseStatistics
import com.github.heheteam.commonlib.DatabaseExceptionError
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.NewScheduledMessageInfo
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.AdminAuthService
import com.github.heheteam.commonlib.logic.CourseTokenService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ScheduledMessageService
import com.github.heheteam.commonlib.util.toUrl
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.types.UserId
import java.time.LocalDateTime

@Suppress(
  "LongParameterList",
  "TooManyFunctions",
) // shortcut to make this not to long; should be fixed later
class AdminApi
internal constructor(
  private val scheduledMessagesService: ScheduledMessageService,
  private val courseStorage: CourseStorage,
  private val adminAuthService: AdminAuthService,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val submissionDistributor: SubmissionDistributor,
  private val personalDeadlinesService: PersonalDeadlinesService,
  private val tokenStorage: CourseTokenService,
) {
  fun sendScheduledMessage(
    adminId: AdminId,
    timestamp: LocalDateTime,
    content: TelegramMessageContent,
    shortName: String,
    courseId: CourseId,
  ): Result<ScheduledMessageId, EduPlatformError> =
    scheduledMessagesService.sendScheduledMessage(
      adminId,
      NewScheduledMessageInfo(timestamp, content, shortName, courseId),
    )

  fun resolveScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<ScheduledMessage, EduPlatformError> =
    scheduledMessagesService.resolveScheduledMessage(scheduledMessageId)

  fun viewScheduledMessages(
    adminId: AdminId? = null,
    courseId: CourseId? = null,
    lastN: Int = 5,
  ): Result<List<ScheduledMessage>, EduPlatformError> =
    scheduledMessagesService.viewScheduledMessages(adminId, courseId, lastN)

  suspend fun deleteScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<Unit, EduPlatformError> =
    scheduledMessagesService.deleteScheduledMessage(scheduledMessageId)

  suspend fun moveAllDeadlinesForStudent(
    studentId: StudentId,
    newDeadline: kotlinx.datetime.LocalDateTime,
  ) {
    personalDeadlinesService.moveDeadlinesForStudent(studentId, newDeadline)
  }

  fun courseExists(courseName: String): Boolean = getCourse(courseName).get() != null

  fun getCourse(courseName: String): Result<Course?, EduPlatformError> =
    courseStorage.getCourses().map { courses -> courses.find { it.name == courseName } }

  fun getCourses(): Result<Map<String, Course>, EduPlatformError> =
    courseStorage.getCourses().map { courses ->
      courses.groupBy { it.name }.mapValues { it.value.first() }
    }

  fun studentExists(id: StudentId): Boolean = studentStorage.resolveStudent(id).isOk

  fun teacherExists(id: TeacherId): Boolean = teacherStorage.resolveTeacher(id).isOk

  fun studiesIn(id: StudentId, course: Course): Boolean =
    courseStorage.getStudentCourses(id).get()?.any { it.id == course.id } ?: false

  fun teachesIn(id: TeacherId, course: Course): Boolean =
    courseStorage.getTeacherCourses(id).get()?.any { it.id == course.id } ?: false

  fun registerStudentForCourse(studentId: StudentId, courseId: CourseId) =
    courseStorage.addStudentToCourse(studentId, courseId)

  fun registerTeacherForCourse(teacherId: TeacherId, courseId: CourseId) =
    courseStorage.addTeacherToCourse(teacherId, courseId)

  fun removeTeacher(teacherId: TeacherId, courseId: CourseId): Boolean =
    courseStorage.removeTeacherFromCourse(teacherId, courseId).isOk

  fun removeStudent(studentId: StudentId, courseId: CourseId): Boolean =
    courseStorage.removeStudentFromCourse(studentId, courseId).isOk

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
  ): Result<AssignmentId, DatabaseExceptionError> =
    assignmentStorage.createAssignment(courseId, description, problemsDescriptions)

  fun createCourse(input: String): Result<CourseId, EduPlatformError> =
    courseStorage.createCourse(input)

  fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, ResolveError<CourseId>> =
    courseStorage.resolveCourseWithSpreadsheetId(courseId)

  fun getCourseStatistics(courseId: CourseId): CourseStatistics {
    val students = courseStorage.getStudents(courseId).value
    val teachers = courseStorage.getTeachers(courseId).value
    val assignments = assignmentStorage.getAssignmentsForCourse(courseId).value

    var totalProblems = 0
    var totalMaxScore = 0
    var totalSubmissions = 0
    var checkedSubmissions = 0
    assignments.forEach { assignment ->
      val problems = problemStorage.getProblemsFromAssignment(assignment.id).value
      totalProblems += problems.size
      totalMaxScore += problems.sumOf { it.maxScore }
      problems.forEach { problem ->
        val submissions = submissionDistributor.getSubmissionsForProblem(problem.id).value
        totalSubmissions += submissions.size
        checkedSubmissions +=
          submissions.count { submissionId ->
            submissionDistributor.isSubmissionAssessed(submissionId).value
          }
      }
    }

    return CourseStatistics(
      studentsCount = students.size,
      teachersCount = teachers.size,
      assignmentsCount = assignments.size,
      totalProblems = totalProblems,
      totalMaxScore = totalMaxScore,
      totalSubmissions = totalSubmissions,
      checkedSubmissions = checkedSubmissions,
      uncheckedSubmissions = totalSubmissions - checkedSubmissions,
      students = students,
      teachers = teachers,
      assignments = assignments,
    )
  }

  fun getRatingLink(courseId: CourseId): Result<String, ResolveError<CourseId>> =
    courseStorage.resolveCourseWithSpreadsheetId(courseId).map { it.second.toUrl() }

  fun loginByTgId(tgId: UserId): Result<Admin, ResolveError<UserId>> =
    adminAuthService.loginByTgId(tgId)

  fun tgIdIsInWhitelist(tgId: UserId): Boolean = adminAuthService.tgIdIsInWhitelist(tgId)

  fun addTgIdToWhitelist(tgId: UserId): Result<Unit, EduPlatformError> =
    adminAuthService.addTgIdToWhitelist(tgId)

  fun createAdmin(name: String, surname: String, tgId: Long): Result<AdminId, EduPlatformError> =
    adminAuthService.createAdmin(name, surname, tgId)

  fun getTokenForCourse(courseId: CourseId): String? = tokenStorage.getTokenForCourse(courseId)

  fun regenerateTokenForCourse(courseId: CourseId): String = tokenStorage.regenerateToken(courseId)
}
