package com.github.heheteam.commonlib.statistics

import java.time.LocalDateTime
import kotlin.time.Duration

interface TeacherStatistics {
  fun recordAssessment(teacherId: String, timestamp: LocalDateTime = LocalDateTime.now())
  fun recordNewSolution(timestamp: LocalDateTime = LocalDateTime.now())
  fun getTeacherStats(teacherId: String): TeacherStatsData
  fun getAllTeachersStats(): Map<String, TeacherStatsData>
  fun getGlobalStats(): GlobalTeacherStats
}

data class TeacherStatsData(
    val totalAssessments: Int,
    val lastAssessmentTime: LocalDateTime?,
    val averageAssessmentsPerDay: Double,
    val averageCheckTimeHours: Double?,
    val uncheckedSolutions: Int,
    val averageResponseTime: Duration?
)

data class GlobalTeacherStats(
    val averageCheckTimeHours: Double,
    val totalUncheckedSolutions: Int,
    val averageResponseTimeHours: Double
)