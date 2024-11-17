package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.*

class StudentCore(
  val solutionDistributor: SolutionDistributor,
  val coursesDistributor: CoursesDistributor,
  val userIdRegistry: UserIdRegistry,
  // is initialized once in the start bot
  var userId: String? = null,
) {
  fun getGradingForAssignment(
    assignment: Assignment,
    course: Course,
  ): List<Pair<Problem, Grade?>> {
    assert(assignment in course.assignments)
    val grades = course.gradeTable.getGradeMap()[Student(userId!!)]
      ?.filter { it.key.assignmentId == assignment.id }
    val gradedProblems = assignment.problems
      .sortedBy { problem -> problem.number }
      .map { problem -> problem to grades?.get(problem) }
    return gradedProblems
  }

  fun getAvailableCourses(): List<Course> {
    return coursesDistributor.getCourses(userId!!)
  }
}
