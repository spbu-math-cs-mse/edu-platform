package com.github.heheteam.commonlib.statistics

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toKotlinDuration

class MockTeacherStatistics : TeacherStatistics {
private val teacherStats = mutableMapOf(
    "teacher1" to mutableListOf(
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now().minusHours(2),
        LocalDateTime.now().minusDays(3)
    ),
    "teacher2" to mutableListOf(
        LocalDateTime.now().minusDays(2),
        LocalDateTime.now().minusHours(3),
        LocalDateTime.now().minusDays(4)
    )
)
private val solutionTimestamps = mutableListOf(
    LocalDateTime.now().minusDays(1).minusDays(5),
    LocalDateTime.now().minusHours(3),
    LocalDateTime.now().minusDays(3).minusHours(5),
    LocalDateTime.now().minusDays(2).minusHours(6),
    LocalDateTime.now().minusDays(0).minusHours(6),
    LocalDateTime.now().minusDays(5).minusHours(3),
    LocalDateTime.now().minusHours(1),
) // TODO delete mock
private var uncheckedSolutions = 2 // TODO read from database

  override fun recordNewSolution(timestamp: LocalDateTime) {
    solutionTimestamps.add(timestamp)
    uncheckedSolutions++
  }

  override fun recordAssessment(teacherId: String, timestamp: LocalDateTime) {
    teacherStats.getOrPut(teacherId) { mutableListOf() }.add(timestamp)
    if (uncheckedSolutions > 0) {
      uncheckedSolutions--
    }
  }

  override fun getTeacherStats(teacherId: String): TeacherStatsData {
    val assessments = teacherStats[teacherId] ?: return TeacherStatsData(
      totalAssessments = 0,
      lastAssessmentTime = null,
      averageAssessmentsPerDay = 0.0,
      averageCheckTimeHours = null,
      uncheckedSolutions = uncheckedSolutions,
      averageResponseTime = null
    )

    val totalAssessments = assessments.size
    val lastAssessment = assessments.maxOrNull()
    
    val averagePerDay = if (assessments.isNotEmpty()) {
      val firstAssessment = assessments.minOrNull()!!
      val daysBetween = 1 + ChronoUnit.DAYS.between(firstAssessment, LocalDateTime.now()).toDouble()
      totalAssessments / daysBetween
    } else 0.0

    val averageCheckTime = if (assessments.size >= 2) {
      val checkTimes = assessments.sorted().windowed(2)
        .map { Duration.between(it[0], it[1]) }
      val totalHours = checkTimes.sumOf { it.toHours() }
      totalHours.toDouble() / checkTimes.size
    } else null

    val averageResponseTime = if (solutionTimestamps.isNotEmpty() && assessments.isNotEmpty()) {
      val responseTimes = solutionTimestamps.map { solutionTime ->
        assessments.filter { it > solutionTime }
          .minOfOrNull { Duration.between(solutionTime, it) }
      }.filterNotNull()
      
      if (responseTimes.isNotEmpty()) {
        Duration.ofMillis(responseTimes.sumOf { it.toMillis() } / responseTimes.size)
      } else null
    } else null

    return TeacherStatsData(
      totalAssessments = totalAssessments,
      lastAssessmentTime = lastAssessment,
      averageAssessmentsPerDay = averagePerDay,
      averageCheckTimeHours = averageCheckTime,
      uncheckedSolutions = uncheckedSolutions,
      averageResponseTime = averageResponseTime?.toKotlinDuration()
    )
  }

  override fun getGlobalStats(): GlobalTeacherStats {
    val allTeacherStats = getAllTeachersStats().values
    
    val avgCheckTime = allTeacherStats
      .mapNotNull { it.averageCheckTimeHours }
      .takeIf { it.isNotEmpty() }
      ?.average() ?: 0.0

    val avgResponseTime = allTeacherStats
      .mapNotNull { it.averageResponseTime }
      .takeIf { it.isNotEmpty() }
      ?.map { it.toDouble(DurationUnit.HOURS) }
      ?.average() ?: 0.0

    return GlobalTeacherStats(
      averageCheckTimeHours = avgCheckTime,
      totalUncheckedSolutions = uncheckedSolutions,
      averageResponseTimeHours = avgResponseTime
    )
  }

  override fun getAllTeachersStats(): Map<String, TeacherStatsData> {
    return teacherStats.keys.associateWith { getTeacherStats(it) }
  }
}
