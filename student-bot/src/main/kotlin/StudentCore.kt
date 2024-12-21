package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
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
) {
  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, Grade?>> {
    val grades =
      gradeTable
        .getStudentPerformance(studentId, listOf(assignmentId), solutionDistributor)
    val gradedProblems =
      problemStorage
        .getProblemsFromAssignment(assignmentId)
        .sortedBy { problem -> problem.number }
        .map { problem -> problem to grades[problem.id] }
    return gradedProblems
  }

  fun getTopGrades(
    courseId: CourseId,
  ): List<Int> {
    val students = getStudentsFromCourse(courseId)
    val assignments = getCourseAssignments(courseId).map { it.id }
    val grades =
      students
        .map { studentId ->
          gradeTable
            .getStudentPerformance(studentId, assignments, solutionDistributor).values.sum()
        }
        .sortedDescending()
        .take(5)
        .filter { it != 0 }
    return grades
  }

  fun getStudentCourses(studentId: StudentId): List<Course> =
    coursesDistributor
      .getStudentCourses(studentId)

  fun getCourseAssignments(courseId: CourseId): List<Assignment> = assignmentStorage.getAssignmentsForCourse(courseId)

  fun addRecord(
    studentId: StudentId,
    courseId: CourseId,
  ) {
    coursesDistributor.addStudentToCourse(studentId, courseId)
  }

  fun getCourses(): List<Course> = coursesDistributor.getCourses()

  fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
  ) {
    solutionDistributor.inputSolution(
      studentId,
      chatId,
      messageId,
      solutionContent,
      problemId,
    )
  }

  fun getCoursesBulletList(studentId: StudentId): String {
    val studentCourses = coursesDistributor.getStudentCourses(studentId)
    val notRegisteredMessage = "Вы не записаны ни на один курс!"
    return if (studentCourses.isNotEmpty()) {
      studentCourses
        .joinToString("\n") { course ->
          "- " + course.name
        }
    } else {
      notRegisteredMessage
    }
  }

  fun getProblemsFromAssignment(assignment: Assignment): List<Problem> =
    problemStorage.getProblemsFromAssignment(assignment.id)

  fun getStudentsFromCourse(courseId: CourseId) = coursesDistributor.getStudents(courseId)
}
