package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.studentbot.logic.NotificationService
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// this class represents a service given by the bot;
// students ids are parameters in this class
@Suppress("LongParameterList")
class StudentCore : KoinComponent {
  private val solutionDistributor: SolutionDistributor by inject()
  private val coursesDistributor: CoursesDistributor by inject()
  private val problemStorage: ProblemStorage by inject()
  private val assignmentStorage: AssignmentStorage by inject()
  private val gradeTable: GradeTable by inject()
  private val notificationService: NotificationService by inject()
  private val botEventBus: BotEventBus by inject()
  private val responsibleTeacherResolver: ResponsibleTeacherResolver by inject()

  init {
    botEventBus.subscribeToGradeEvents { studentId, chatId, messageId, assessment, problem ->
      notifyAboutGrade(studentId, chatId, messageId, assessment, problem)
    }
  }

  fun getGradingForAssignment(
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

  fun getTopGrades(courseId: CourseId): List<Int> {
    val students = getStudentsFromCourse(courseId).map { it.id }
    val assignments = getCourseAssignments(courseId).map { it.id }
    val grades =
      students
        .map { studentId ->
          gradeTable.getStudentPerformance(studentId, assignments).values.filterNotNull().sum()
        }
        .sortedDescending()
        .take(5)
        .filter { it != 0 }
    return grades
  }

  fun getStudentCourses(studentId: StudentId): List<Course> =
    coursesDistributor.getStudentCourses(studentId)

  fun getCourseAssignments(courseId: CourseId): List<Assignment> =
    assignmentStorage.getAssignmentsForCourse(courseId)

  fun addRecord(studentId: StudentId, courseId: CourseId) =
    coursesDistributor.addStudentToCourse(studentId, courseId)

  fun getCourses(): List<Course> = coursesDistributor.getCourses()

  fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
  ) {
    val teacher = responsibleTeacherResolver.resolveResponsibleTeacher(problemId)
    val solutionId =
      solutionDistributor.inputSolution(
        studentId,
        chatId,
        messageId,
        solutionContent,
        problemId,
        LocalDateTime.now(),
        teacher.get(),
      )
    solutionDistributor.resolveSolution(solutionId).map { solution: Solution ->
      botEventBus.publishNewSolutionEvent(solution)
    }
  }

  fun getProblemsFromAssignment(assignment: Assignment): List<Problem> =
    problemStorage.getProblemsFromAssignment(assignment.id)

  suspend fun notifyAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) {
    notificationService.notifyStudentAboutGrade(studentId, chatId, messageId, assessment, problem)
  }

  fun getStudentsFromCourse(courseId: CourseId) = coursesDistributor.getStudents(courseId)
}
