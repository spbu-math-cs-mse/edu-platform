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
import com.github.heheteam.commonlib.api.NotificationService
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.michaelbull.result.map
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

// this class represents a service given by the bot;
// students ids are parameters in this class
class StudentCore(
  private val solutionDistributor: SolutionDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val gradeTable: GradeTable,
  private val notificationService: NotificationService,
  private val botEventBus: BotEventBus,
) {
  init {
    botEventBus.subscribeToGradeEvents { studentId, chatId, messageId, assessment, problem ->
      notifyAboutGrade(studentId, chatId, messageId, assessment, problem)
    }
  }

  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, Grade?>> {
    val grades = gradeTable.getStudentPerformance(studentId, listOf(assignmentId))
    val gradedProblems =
      problemStorage
        .getProblemsFromAssignment(assignmentId)
        .sortedBy { problem -> problem.number }
        .map { problem -> problem to grades[problem.id] }
    return gradedProblems
  }

  fun getTopGrades(courseId: CourseId): List<Int> {
    val students = getStudentsFromCourse(courseId).map { it.id }
    val assignments = getCourseAssignments(courseId).map { it.id }
    val grades =
      students
        .map { studentId -> gradeTable.getStudentPerformance(studentId, assignments).values.sum() }
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
    val solutionId =
      solutionDistributor.inputSolution(studentId, chatId, messageId, solutionContent, problemId)
    solutionDistributor.resolveSolution(solutionId).map { solution: Solution ->
      botEventBus.publishNewSolutionEvent(solution)
    }
  }

  fun getCoursesBulletList(studentId: StudentId): String {
    val studentCourses = coursesDistributor.getStudentCourses(studentId)
    val notRegisteredMessage = "Вы не записаны ни на один курс!"
    return if (studentCourses.isNotEmpty()) {
      studentCourses.joinToString("\n") { course -> "- " + course.name }
    } else {
      notRegisteredMessage
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
