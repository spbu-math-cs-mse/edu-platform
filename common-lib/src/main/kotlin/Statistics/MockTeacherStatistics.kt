package com.github.heheteam.commonlib.statistics

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MockTeacherStatistics : TeacherStatistics {
  private val teacherStats = mutableMapOf<String, MutableList<LocalDateTime>>()

  override fun recordAssessment(teacherId: String, timestamp: LocalDateTime) {
    teacherStats.getOrPut(teacherId) { mutableListOf() }.add(timestamp)
  }

  override fun getTeacherStats(teacherId: String): TeacherStatsData {
    val assessments = teacherStats[teacherId] ?: return TeacherStatsData(0, null, 0.0)

    val totalAssessments = assessments.size
    val lastAssessment = assessments.maxOrNull()

    val averagePerDay = if (assessments.isNotEmpty()) {
      val firstAssessment = assessments.minOrNull()!!
      val daysBetween = 1 + ChronoUnit.DAYS.between(firstAssessment, LocalDateTime.now()).toDouble()
      totalAssessments / daysBetween
    } else 0.0

    return TeacherStatsData(totalAssessments, lastAssessment, averagePerDay)
  }

  override fun getAllTeachersStats(): Map<String, TeacherStatsData> {
    return teacherStats.keys.associateWith { getTeacherStats(it) }
  }
}
