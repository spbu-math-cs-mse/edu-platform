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
    val assignment = assignmentStorage.resolveAssignment(assignmentId)
    val courseId = coursesDistributor.resolveCourse(assignment.courseId)!!
    val grades =
      gradeTable
        .getStudentPerformance(studentId, solutionDistributor)
        .filter { problemStorage.resolveProblem(it.key).assignmentId == assignmentId }
    val gradedProblems =
      assignment.problemIds
        .map { problemStorage.resolveProblem(it) }
        .sortedBy { problem -> problem.number }
        .map { problem -> problem to grades[problem.id] }
    return gradedProblems
  }

  fun getStudentCourses(studentId: StudentId): List<Course> =
    coursesDistributor
      .getStudentCourses(studentId)
      .map { coursesDistributor.resolveCourse(it)!! }

  fun getCourseAssignments(courseId: CourseId): List<Assignment> =
    assignmentStorage
      .getAssignmentsForCourse(courseId)
      .map { assignmentStorage.resolveAssignment(it) }

  fun addRecord(
    studentId: StudentId,
    courseId: CourseId,
  ) {
    coursesDistributor.addRecord(studentId, courseId)
  }

  fun getCourses(): List<Course> =
    coursesDistributor
      .getCourses()
      .map { coursesDistributor.resolveCourse(it)!! }

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
        .joinToString("\n") { courseId ->
          "- " +
            coursesDistributor
              .resolveCourse(
                courseId,
              )!!
              .description
        }
    } else {
      notRegisteredMessage
    }
  }

  fun getProblemsFromAssignment(assignment: Assignment): List<Problem> =
    problemStorage
      .getProblemsFromAssignment(assignment.id)
      .map { problemStorage.resolveProblem(it) }
}
