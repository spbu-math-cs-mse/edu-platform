package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatsData

class TeacherCore(
  private val teacherStatistics: TeacherStatistics,
  private val coursesDistributor: CoursesDistributor,
) {
  fun getTeacherStats(teacherId: String): TeacherStatsData? = teacherStatistics.getTeacherStats(teacherId)

  fun getGlobalStats() = teacherStatistics.getGlobalStats()

  fun getAvailableCourses(teacherId: String): List<Course> {
    return coursesDistributor.getTeacherCourses(teacherId)
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
