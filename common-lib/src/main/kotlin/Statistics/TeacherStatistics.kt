package com.github.heheteam.commonlib.statistics

import com.github.heheteam.commonlib.Solution
import java.time.LocalDateTime
import kotlin.time.Duration

interface TeacherStatistics {
  fun recordAssessment(teacherId: String, solution: Solution, timestamp: LocalDateTime)
  fun recordNewSolution(solution: Solution)
  fun getTeacherStats(teacherId: String): TeacherStatsData?
  fun getAllTeachersStats(): Map<String, TeacherStatsData>
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