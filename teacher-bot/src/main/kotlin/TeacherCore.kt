package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import java.time.LocalDateTime

class TeacherCore(
  private val teacherStatistics: TeacherStatistics,
  private val coursesDistributor: CoursesDistributor,
  private val solutionDistributor: SolutionDistributor,
  private val gradeTable: GradeTable,
  private val problemStorage: ProblemStorage,
  private val botEventBus: BotEventBus,
) {
  fun getTeacherStats(teacherId: TeacherId): TeacherStatsData? {
    val result = teacherStatistics.resolveTeacherStats(teacherId)
    return result.get()
  }

  fun getGlobalStats() = teacherStatistics.getGlobalStats()

  fun getQueryStats() = teacherStatistics.getGlobalStats()

  fun getAvailableCourses(teacherId: TeacherId): List<Course> = coursesDistributor.getTeacherCourses(teacherId)

  fun querySolution(teacherId: TeacherId): Solution? = solutionDistributor.querySolution(teacherId, gradeTable).value

  fun assessSolution(
    solution: Solution,
    teacherId: TeacherId,
    assessment: SolutionAssessment,
    timestamp: LocalDateTime = LocalDateTime.now(),
  ) {
    gradeTable.assessSolution(
      solution.id,
      teacherId,
      assessment,
      gradeTable,
      teacherStatistics,
      timestamp,
    )

    problemStorage.resolveProblem(solution.problemId).map { problem ->
      botEventBus.publishGradeEvent(
        solution.studentId, solution.chatId, solution.messageId, assessment, problem
      )
    }
  }

  fun getGrading(course: Course): List<Pair<StudentId, Grade>> {
    val students = coursesDistributor.getStudents(course.id)
    val grades = students.map { studentId ->
      studentId to gradeTable.getStudentPerformance(studentId, solutionDistributor).values.sum()
    }
    return grades
  }

  fun getMaxGrade(course: Course): Grade = 5 // TODO: this needs to be fixed properly
}
