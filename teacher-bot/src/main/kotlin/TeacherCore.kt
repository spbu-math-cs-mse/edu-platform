package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatsData

class TeacherCore(
  private val teacherStatistics: TeacherStatistics,
  private val coursesDistributor: CoursesDistributor,
  private val solutionDistributor: SolutionDistributor,
) {
  fun getTeacherStats(teacherId: String): TeacherStatsData? = teacherStatistics.getTeacherStats(teacherId)

  fun getGlobalStats() = teacherStatistics.getGlobalStats()

  fun getQueryStats() = teacherStatistics.getGlobalStats()

  fun getAvailableCourses(teacherId: String): List<Course> {
    return coursesDistributor.getTeacherCourses(teacherId)
  }

  fun querySolution(teacherId: String): Solution? {
    return solutionDistributor.querySolution(teacherId)
  }

  fun assessSolution(
    solution: Solution,
    teacherId: String,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now(),
  ) {
    solutionDistributor.assessSolution(solution,teacherId,assessment, gradeTable, timestamp)
  }

  fun getGrading(course: Course): List<Pair<Student, Grade?>> {
    return course.gradeTable.getGradeMap().map { (student, solvedProblems) ->
      student to solvedProblems.filter { (problem: Problem, _: Grade) ->
        course.assignments.map { it.id }.contains(problem.assignmentId)
      }.map { (_: Problem, grade: Grade) -> grade }.sum()
    }
  }

  fun getMaxGrade(course: Course): Grade {
    return course.assignments.flatMap { it.problems }.sumOf { it.maxScore }
  }
}
