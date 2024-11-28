package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatsData
import java.time.LocalDateTime

class TeacherCore(
  private val teacherStatistics: TeacherStatistics,
  private val coursesDistributor: CoursesDistributor,
  private val solutionDistributor: SolutionDistributor,
) {
  fun getTeacherStats(teacherId: Long): TeacherStatsData? =
    teacherStatistics.getTeacherStats(teacherId)

  fun getGlobalStats() = teacherStatistics.getGlobalStats()

  fun getQueryStats() = teacherStatistics.getGlobalStats()

  fun getAvailableCourses(teacherId: Long): List<Course> {
    return coursesDistributor.getTeacherCourses(teacherId)
      .map { coursesDistributor.resolveCourse(it)!! }
  }

  fun querySolution(teacherId: Long): Solution? {
    return solutionDistributor.querySolution(teacherId)
      ?.let { solutionDistributor.resolveSolution(it) }
  }

  fun assessSolution(
    solution: Solution,
    teacherId: Long,
    assessment: SolutionAssessment,
    gradeTable: GradeTable,
    timestamp: LocalDateTime = LocalDateTime.now(),
  ) {
    solutionDistributor.assessSolution(
      solution.id,
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
      student to course.gradeTable.getStudentPerformance(student.id, solutionDistributor).values.sum()
    }
    return grades
  }

  fun getMaxGrade(course: Course): Grade {
    return 5
  }
}
