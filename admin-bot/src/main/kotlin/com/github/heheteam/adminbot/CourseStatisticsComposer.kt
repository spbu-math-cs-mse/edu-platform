package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor

class CourseStatisticsComposer(
  private val coursesDistributor: CoursesDistributor,
  private val assignmentStorage: AssignmentStorage,
  private val problemStorage: ProblemStorage,
  private val solutionDistributor: SolutionDistributor,
) {
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
