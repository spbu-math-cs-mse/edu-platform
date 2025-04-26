package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.CourseStatistics
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.AdminStorage
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CoursesDistributor
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ScheduledMessage
import com.github.heheteam.commonlib.interfaces.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.util.toUrl
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.types.UserId
import java.time.LocalDateTime

@Suppress(
  "LongParameterList",
  "TooManyFunctions",
) // shortcut to make this not to long; should be fixed later
class AdminApi
internal constructor(
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val adminStorage: AdminStorage,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val solutionDistributor: SolutionDistributor,
  private val personalDeadlinesService: PersonalDeadlinesService,
) {
  fun addMessage(message: ScheduledMessage) = scheduledMessagesDistributor.addMessage(message)

  fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> =
    scheduledMessagesDistributor.getMessagesUpToDate(date)

  fun markMessagesUpToDateAsSent(date: LocalDateTime) =
    scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  fun moveAllDeadlinesForStudent(
    studentId: StudentId,
    newDeadline: kotlinx.datetime.LocalDateTime,
  ) {
    personalDeadlinesService.moveDeadlinesForStudent(studentId, newDeadline)
  }

  fun courseExists(courseName: String): Boolean = getCourse(courseName) != null

  fun getCourse(courseName: String): Course? =
    coursesDistributor.getCourses().find { it.name == courseName }

  fun getCourses(): Map<String, Course> =
    coursesDistributor.getCourses().groupBy { it.name }.mapValues { it.value.first() }

  fun studentExists(id: StudentId): Boolean = studentStorage.resolveStudent(id).isOk

  fun teacherExists(id: TeacherId): Boolean = teacherStorage.resolveTeacher(id).isOk

  fun studiesIn(id: StudentId, course: Course): Boolean =
    coursesDistributor.getStudentCourses(id).any { it.id == course.id }

  fun teachesIn(id: TeacherId, course: Course): Boolean =
    coursesDistributor.getTeacherCourses(id).any { it.id == course.id }

  fun registerStudentForCourse(studentId: StudentId, courseId: CourseId) =
    coursesDistributor.addStudentToCourse(studentId, courseId)

  fun registerTeacherForCourse(teacherId: TeacherId, courseId: CourseId) =
    coursesDistributor.addTeacherToCourse(teacherId, courseId)

  fun removeTeacher(teacherId: TeacherId, courseId: CourseId): Boolean =
    coursesDistributor.removeTeacherFromCourse(teacherId, courseId).isOk

  fun removeStudent(studentId: StudentId, courseId: CourseId): Boolean =
    coursesDistributor.removeStudentFromCourse(studentId, courseId).isOk

  fun createAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
  ) = assignmentStorage.createAssignment(courseId, description, problemsDescriptions)

  fun createCourse(input: String): CourseId = coursesDistributor.createCourse(input)

  fun resolveCourseWithSpreadsheetId(
    courseId: CourseId
  ): Result<Pair<Course, SpreadsheetId>, ResolveError<CourseId>> =
    coursesDistributor.resolveCourseWithSpreadsheetId(courseId)

  fun getCourseStatistics(courseId: CourseId): CourseStatistics {
    val students = coursesDistributor.getStudents(courseId)
    val teachers = coursesDistributor.getTeachers(courseId)
    val assignments = assignmentStorage.getAssignmentsForCourse(courseId)

    var totalProblems = 0
    var totalMaxScore = 0
    var totalSolutions = 0
    var checkedSolutions = 0
    assignments.forEach { assignment ->
      val problems = problemStorage.getProblemsFromAssignment(assignment.id)
      totalProblems += problems.size
      totalMaxScore += problems.sumOf { it.maxScore }
      problems.forEach { problem ->
        val solutions = solutionDistributor.getSolutionsForProblem(problem.id)
        totalSolutions += solutions.size
        checkedSolutions +=
          solutions.count { solutionId -> solutionDistributor.isSolutionAssessed(solutionId) }
      }
    }

    return CourseStatistics(
      studentsCount = students.size,
      teachersCount = teachers.size,
      assignmentsCount = assignments.size,
      totalProblems = totalProblems,
      totalMaxScore = totalMaxScore,
      totalSolutions = totalSolutions,
      checkedSolutions = checkedSolutions,
      uncheckedSolutions = totalSolutions - checkedSolutions,
      students = students,
      teachers = teachers,
      assignments = assignments,
    )
  }

  fun getRatingLink(courseId: CourseId): Result<String, ResolveError<CourseId>> =
    coursesDistributor.resolveCourseWithSpreadsheetId(courseId).map { it.second.toUrl() }

  fun loginByTgId(tgId: UserId): Result<Admin, ResolveError<UserId>> =
    adminStorage.resolveByTgId(tgId)

  fun loginById(adminId: AdminId): Result<Admin, ResolveError<AdminId>> =
    adminStorage.resolveAdmin(adminId)

  fun updateTgId(adminId: AdminId, newTgId: UserId): Result<Unit, ResolveError<AdminId>> =
    adminStorage.updateTgId(adminId, newTgId)

  fun createStudent(name: String, surname: String, tgId: Long): AdminId =
    adminStorage.createAdmin(name, surname, tgId)
}
