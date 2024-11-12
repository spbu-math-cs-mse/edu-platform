package com.github.heheteam.commonlib.statistics

import java.time.LocalDateTime

interface TeacherStatistics {
  fun recordAssessment(teacherId: String, timestamp: LocalDateTime = LocalDateTime.now())
  fun getTeacherStats(teacherId: String): TeacherStatsData
  fun getAllTeachersStats(): Map<String, TeacherStatsData>
}

data class TeacherStatsData(
  val totalAssessments: Int,
  val lastAssessmentTime: LocalDateTime?,
  val averageAssessmentsPerDay: Double,
)
