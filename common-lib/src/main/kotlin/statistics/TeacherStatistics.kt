package com.github.heheteam.commonlib.statistics

import com.github.heheteam.commonlib.Solution
import java.time.LocalDateTime

interface TeacherStatistics {
  fun recordAssessment(teacherId: Long, solution: Solution, timestamp: LocalDateTime)
  fun recordNewSolution(solution: Solution)
  fun getTeacherStats(teacherId: Long): TeacherStatsData?
  fun getAllTeachersStats(): Map<Long, TeacherStatsData>
  fun getGlobalStats(): GlobalTeacherStats
}

data class TeacherStatsData(
  val totalAssessments: Int,
  val lastAssessmentTime: LocalDateTime,
  val averageAssessmentsPerDay: Double,
  val averageCheckTimeSeconds: Double,
)

data class GlobalTeacherStats(
  val averageCheckTimeHours: Double,
  val totalUncheckedSolutions: Int,
)
