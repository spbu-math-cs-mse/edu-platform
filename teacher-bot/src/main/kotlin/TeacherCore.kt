package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatsData

class TeacherCore(
  private val teacherStatistics: TeacherStatistics,
  private val coursesDistributor: CoursesDistributor,
  private val solutionDistributor: SolutionDistributor,
) {
  fun getTeacherStats(teacherId: String): TeacherStatsData? =
    teacherStatistics.getTeacherStats(teacherId)

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
    solutionDistributor.assessSolution(
      solution,
      teacherId,
      assessment,
      gradeTable,
      timestamp,
      teacherStatistics,
    )
  }

  fun getGrading(course: Course): List<Pair<Student, Grade?>> {
    val students = course.students
    val grades = students.map { student ->
      student to course.gradeTable.getStudentPerformance(student.id).values.sum()
    }
    return grades
  }

  fun getMaxGrade(course: Course): Grade {
    return course.assignments.flatMap { it.problems }.sumOf { it.maxScore }
  }
}
