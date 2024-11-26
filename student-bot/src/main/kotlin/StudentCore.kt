package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

// this class represents a service given by the bot;
// students ids are parameters in this class
class StudentCore(
  private val solutionDistributor: SolutionDistributor,
  private val coursesDistributor: CoursesDistributor,
) {
  fun getGradingForAssignment(
    assignment: Assignment,
    course: Course,
    studentId: String,
  ): List<Pair<Problem, Grade?>> {
    assert(assignment in course.assignments)
    val grades =
      course.gradeTable.getStudentPerformance(studentId)
        .filter { it.key.assignmentId == assignment.id }
    val gradedProblems =
      assignment.problems
        .sortedBy { problem -> problem.number }
        .map { problem -> problem to grades[problem] }
    return gradedProblems
  }

  fun getStudentCourses(studentId: String): List<Course> {
    return coursesDistributor.getStudentCourses(studentId)
  }

  fun addRecord(studentId: String, courseId: String) {
    coursesDistributor.addRecord(studentId, courseId)
  }

  fun getCourses(): List<Course> {
    return coursesDistributor.getCourses()
  }

  fun inputSolution(
    studentId: String,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
  ) {
    solutionDistributor.inputSolution(
      studentId,
      chatId,
      messageId,
      solutionContent,
    )
  }

  fun getCoursesBulletList(userId: String): String {
    val studentCourses = coursesDistributor.getStudentCourses(userId)
    val notRegisteredMessage = "Вы не записаны ни на один курс!"
    return if (!studentCourses.isEmpty()) {
      studentCourses
        .joinToString("\n") { course -> "- " + course.description }
    } else {
      notRegisteredMessage
    }
  }
}
