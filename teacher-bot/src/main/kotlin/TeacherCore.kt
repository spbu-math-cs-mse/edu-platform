package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatsData
import java.time.LocalDateTime

class TeacherCore(
  private val teacherStatistics: TeacherStatistics,
  private val coursesDistributor: CoursesDistributor,
  private val solutionDistributor: SolutionDistributor,
  private val gradeTable: GradeTable,
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
      teacherStatistics,
      timestamp,
    )
  }

  fun getGrading(course: Course): List<Pair<StudentId, Grade?>> {
    val students = coursesDistributor.getStudents(course.id)
    val grades = students.map { studentId ->
      studentId to gradeTable.getStudentPerformance(studentId, solutionDistributor).values.sum()
    }
    return grades
  }

  fun getMaxGrade(course: Course): Grade {
    return 5
  }
}
