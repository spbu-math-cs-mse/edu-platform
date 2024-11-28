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
) {
  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: Long,
  ): List<Pair<Problem, Grade?>> {
    val assignment = assignmentStorage.resolveAssignment(assignmentId)
    val course = coursesDistributor.resolveCourse(assignment.courseId)!!
    assert(assignmentId in course.assignmentIds)
    val grades =
      course.gradeTable.getStudentPerformance(studentId, solutionDistributor)
        .filter { problemStorage.resolveProblem(it.key).assignmentId == assignmentId }
    val gradedProblems =
      assignment.problemIds
        .map { problemStorage.resolveProblem(it) }
        .sortedBy { problem -> problem.number }
        .map { problem -> problem to grades[problem.id] }
    return gradedProblems
  }

  fun getStudentCourses(studentId: Long): List<Course> {
    return coursesDistributor.getStudentCourses(studentId)
      .map { coursesDistributor.resolveCourse(it)!! }
  }

  fun getCourseAssignments(course: Course): List<Assignment> {
    return course.assignmentIds.map { assignmentStorage.resolveAssignment(it) }
  }

  fun addRecord(studentId: Long, courseId: Long) {
    coursesDistributor.addRecord(studentId, courseId)
  }

  fun getCourses(): List<Course> {
    return coursesDistributor.getCourses()
      .map { coursesDistributor.resolveCourse(it)!! }
  }

  fun inputSolution(
    studentId: Long,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
  ) {
    TODO("broken")
//    solutionDistributor.inputSolution(studentId, chatId, messageId, solutionContent, )
  }

  fun getCoursesBulletList(userId: Long): String {
    val studentCourses = coursesDistributor.getStudentCourses(userId)
    val notRegisteredMessage = "Вы не записаны ни на один курс!"
    return if (studentCourses.isNotEmpty()) {
      studentCourses
        .joinToString("\n") { courseId ->
          "- " + coursesDistributor.resolveCourse(
            courseId,
          )!!.description
        }
    } else {
      notRegisteredMessage
    }
  }
}
