package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ScheduledMessage
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import dev.inmo.tgbotapi.types.message.textsources.RegularTextSource
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.utils.RiskFeature
import java.time.LocalDateTime

class AdminCore(
  private val scheduledMessagesDistributor: ScheduledMessagesDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val solutionDistributor: SolutionDistributor,
) {
  fun addMessage(message: ScheduledMessage) = scheduledMessagesDistributor.addMessage(message)

  fun getMessagesUpToDate(date: LocalDateTime): List<ScheduledMessage> =
    scheduledMessagesDistributor.getMessagesUpToDate(date)

  fun markMessagesUpToDateAsSent(date: LocalDateTime) =
    scheduledMessagesDistributor.markMessagesUpToDateAsSent(date)

  fun courseExists(courseName: String): Boolean = getCourse(courseName) != null

  fun addCourse(courseName: String) = coursesDistributor.createCourse(courseName)

  fun addAssignment(
    courseId: CourseId,
    description: String,
    problemsDescriptions: List<ProblemDescription>,
  ) {
    assignmentStorage.createAssignment(courseId, description, problemsDescriptions, problemStorage)
  }

  fun getCourse(courseName: String): Course? =
    coursesDistributor.getCourses().find { it.name == courseName }

  fun getCourses(): Map<String, Course> =
    coursesDistributor.getCourses().groupBy { it.name }.mapValues { it.value.first() }

  fun studentExists(id: StudentId): Boolean = studentStorage.resolveStudent(id).isOk

  fun teacherExists(id: TeacherId): Boolean = teacherStorage.resolveTeacher(id).isOk

  fun studiesIn(id: StudentId, course: Course): Boolean =
    coursesDistributor.getStudentCourses(id).find { it.id == course.id } != null

  fun teachesIn(id: TeacherId, course: Course): Boolean =
    coursesDistributor.getTeacherCourses(id).find { it.id == course.id } != null

  fun registerStudentForCourse(studentId: StudentId, courseId: CourseId) =
    coursesDistributor.addStudentToCourse(studentId, courseId)

  fun registerTeacherForCourse(teacherId: TeacherId, courseId: CourseId) {
    coursesDistributor.addTeacherToCourse(teacherId, courseId)
  }

  fun removeTeacher(teacherId: TeacherId, courseId: CourseId): Boolean =
    coursesDistributor.removeTeacherFromCourse(teacherId, courseId).isOk

  fun removeStudent(studentId: StudentId, courseId: CourseId): Boolean =
    coursesDistributor.removeStudentFromCourse(studentId, courseId).isOk

  fun getTeachersBulletList(): String {
    val teachersList = teacherStorage.getTeachers()
    val noTeachers = "Список преподавателей пуст!"
    return if (teachersList.isNotEmpty()) {
      teachersList
        .sortedBy { it.surname }
        .joinToString("\n") { teacher -> "- ${teacher.surname} ${teacher.name}" }
    } else {
      noTeachers
    }
  }

  @OptIn(RiskFeature::class)
  fun getProblemsEntitiesList(course: Course): List<TextSource> {
    val assignmentsList = assignmentStorage.getAssignmentsForCourse(course.id)
    val noAssignments = "Список серий пуст!"
    return if (assignmentsList.isNotEmpty()) {
      val entitiesList: MutableList<TextSource> = mutableListOf()
      assignmentsList.forEachIndexed { index, assignment ->
        val problemsList = problemStorage.getProblemsFromAssignment(assignment.id)
        val noProblems = "Задачи в этой серии отсутствуют."
        val problems =
          if (problemsList.isNotEmpty()) {
            problemsList.joinToString("\n") { problem ->
              "    \uD83C\uDFAF задача ${problem.number}"
            }
          } else {
            noProblems
          }
        entitiesList.addAll(
          listOf(
            RegularTextSource("\uD83D\uDCDA "),
            bold(assignment.description),
            RegularTextSource(
              ":\n$problems${if (index == (assignmentsList.size - 1)) "" else "\n\n"}"
            ),
          )
        )
      }
      entitiesList
    } else {
      listOf(RegularTextSource(noAssignments))
    }
  }

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
}
