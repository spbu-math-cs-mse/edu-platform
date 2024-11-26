package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.*

// this class represents a service given by the bot;
// students ids are parameters in this class
class StudentCore(
  val solutionDistributor: SolutionDistributor,
  val coursesDistributor: CoursesDistributor,
) {
  fun getGradingForAssignment(
    assignment: Assignment,
    course: Course,
    studentId: String,
  ): List<Pair<Problem, Grade?>> {
    assert(assignment in course.assignments)
    val grades = course.gradeTable.getGradeMap()[Student(studentId)]
      ?.filter { it.key.assignmentId == assignment.id }
    val gradedProblems = assignment.problems
      .sortedBy { problem -> problem.number }
      .map { problem -> problem to grades?.get(problem) }
    return gradedProblems
  }

  fun getAvailableCourses(studentId: String): List<Course> {
    return coursesDistributor.getStudentCourses(studentId)
  }

  fun addRecord(studentId: String, courseId: String) {
    coursesDistributor.addRecord(studentId, courseId)
  }

  fun getCourses(): List<Course> {
    return coursesDistributor.getCourses()
  }
}
