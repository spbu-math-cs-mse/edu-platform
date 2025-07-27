package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.CourseStatistics
import com.github.heheteam.commonlib.NewScheduledMessageInfo
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.ScheduledMessage
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.domain.AddStudentStatus
import com.github.heheteam.commonlib.domain.RemoveStudentStatus
import com.github.heheteam.commonlib.errors.CourseService
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.AdminAuthService
import com.github.heheteam.commonlib.logic.CourseTokenService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ScheduledMessageService
import com.github.heheteam.commonlib.logic.UserGroup
import com.github.heheteam.commonlib.util.toUrl
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.types.RawChatId
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
  private val teacherStorage: TeacherStorage,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val submissionDistributor: SubmissionDistributor,
  private val personalDeadlinesService: PersonalDeadlinesService,
  private val tokenStorage: CourseTokenService,
  private val errorManagementService: ErrorManagementService,
  private val courseService: CourseService,
) {
  fun sendScheduledMessage(
    adminId: AdminId,
    timestamp: LocalDateTime,
    content: TelegramMessageContent,
    shortName: String,
    userGroup: UserGroup,
  ): Result<ScheduledMessageId, NumberedError> =
    errorManagementService.serviceBinding {
      scheduledMessagesService
        .sendScheduledMessage(
          adminId,
          NewScheduledMessageInfo(timestamp, content, shortName, userGroup),
        )
        .bind()
    }

  fun resolveScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<ScheduledMessage, NumberedError> =
    errorManagementService.serviceBinding {
      scheduledMessagesService.resolveScheduledMessage(scheduledMessageId).bind()
    }

  fun addStudents(
    courseId: CourseId,
    students: List<StudentId>,
  ): Result<List<AddStudentStatus>, NumberedError> =
    errorManagementService.serviceBinding { courseService.addStudents(courseId, students).bind() }

  fun removeStudents(
    courseId: CourseId,
    students: List<StudentId>,
  ): Result<List<RemoveStudentStatus>, NumberedError> =
    errorManagementService.serviceBinding {
      courseService.removeStudents(courseId, students).bind()
    }

  fun viewScheduledMessages(
    adminId: AdminId? = null,
    courseId: CourseId? = null,
    lastN: Int = 5,
  ): Result<List<ScheduledMessage>, NumberedError> =
    errorManagementService.serviceBinding {
      scheduledMessagesService.viewScheduledMessages(adminId, courseId, lastN).bind()
    }

  suspend fun deleteScheduledMessage(
    scheduledMessageId: ScheduledMessageId
  ): Result<Unit, NumberedError> =
    errorManagementService.coroutineServiceBinding {
      scheduledMessagesService.deleteScheduledMessage(scheduledMessageId).bind()
    }

  suspend fun moveAllDeadlinesForStudent(
    studentId: StudentId,
    newDeadline: kotlinx.datetime.LocalDateTime,
  ) {
    personalDeadlinesService.moveDeadlinesForStudent(studentId, newDeadline)
  }

  fun getCourse(courseName: String): Result<Course?, NumberedError> =
    errorManagementService.serviceBinding {
      courseStorage.getCourses().bind().find { it.name == courseName }
    }

  fun getCourses(): Result<Map<String, Course>, NumberedError> =
    errorManagementService.serviceBinding {
      courseStorage.getCourses().bind().groupBy { it.name }.mapValues { it.value.first() }
    }

  fun teacherExists(id: TeacherId): Boolean = teacherStorage.resolveTeacher(id).isOk

  fun teachesIn(id: TeacherId, course: Course): Boolean =
    courseStorage.getTeacherCourses(id).get()?.any { it.id == course.id } ?: false

  fun registerStudentForCourse(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Unit, NumberedError> =
    errorManagementService.serviceBinding {
      courseStorage.addStudentToCourse(studentId, courseId).bind()
    }

  fun registerTeacherForCourse(
    teacherId: TeacherId,
    courseId: CourseId,
  ): Result<Unit, NumberedError> =
    errorManagementService.serviceBinding {
      courseStorage.addTeacherToCourse(teacherId, courseId).bind()
    }

  fun removeTeacher(teacherId: TeacherId, courseId: CourseId): Boolean =
    courseStorage.removeTeacherFromCourse(teacherId, courseId).isOk

  fun removeStudent(studentId: StudentId, courseId: CourseId): Boolean =
    courseStorage.removeStudentFromCourse(studentId, courseId).isOk

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
  ): Result<AssignmentId, NumberedError> =
    errorManagementService.serviceBinding {
      assignmentStorage.createAssignment(courseId, description, problemsDescriptions).bind()
    }

  fun createCourse(input: String): Result<CourseId, NumberedError> =
    errorManagementService.serviceBinding { courseStorage.createCourse(input).bind() }

  fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, NumberedError> =
    errorManagementService.serviceBinding {
      courseStorage.resolveCourseWithSpreadsheetId(courseId).bind()
    }

  fun getCourseStatistics(courseId: CourseId): Result<CourseStatistics, FrontendError> =
    errorManagementService.serviceBinding {
      val students = courseStorage.getStudents(courseId).bind()
      val teachers = courseStorage.getTeachers(courseId).bind()
      val assignments = assignmentStorage.getAssignmentsForCourse(courseId).bind()
      var totalProblems = 0
      var totalMaxScore = 0
      var totalSubmissions = 0
      var checkedSubmissions = 0
      assignments.forEach { assignment ->
        val problems = problemStorage.getProblemsFromAssignment(assignment.id).bind()
        totalProblems += problems.size
        totalMaxScore += problems.sumOf { it.maxScore }
        problems.forEach { problem ->
          val submissions = submissionDistributor.getSubmissionsForProblem(problem.id).bind()
          totalSubmissions += submissions.size
          checkedSubmissions +=
            submissions.count { submissionId ->
              submissionDistributor.isSubmissionAssessed(submissionId).bind()
            }
        }
      }
      CourseStatistics(
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

  fun getRatingLink(courseId: CourseId): Result<String, NumberedError> =
    errorManagementService.serviceBinding {
      courseStorage.resolveCourseWithSpreadsheetId(courseId).bind().second.toUrl()
    }

  fun loginByTgId(tgId: UserId): Result<Admin?, NumberedError> =
    errorManagementService.serviceBinding { adminAuthService.loginByTgId(tgId).bind() }

  fun tgIdIsInWhitelist(tgId: UserId): Result<Boolean, NumberedError> =
    errorManagementService.serviceBinding { adminAuthService.tgIdIsInWhitelist(tgId).bind() }

  fun addTgIdToWhitelist(tgId: UserId): Result<Unit, NumberedError> =
    errorManagementService.serviceBinding { adminAuthService.addTgIdToWhitelist(tgId).bind() }

  fun createAdmin(name: String, surname: String, tgId: Long): Result<AdminId, NumberedError> =
    errorManagementService.serviceBinding {
      adminAuthService.createAdmin(name, surname, tgId).bind()
    }

  fun getTokenForCourse(courseId: CourseId): String? = tokenStorage.getTokenForCourse(courseId)

  fun regenerateTokenForCourse(courseId: CourseId): String = tokenStorage.regenerateToken(courseId)

  fun bindErrorChat(id: RawChatId) {
    errorManagementService.boundChat(id)
  }
}
