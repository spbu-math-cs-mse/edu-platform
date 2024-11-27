package com.github.heheteam.commonlib.statistics

import com.github.heheteam.commonlib.Solution
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private data class SolutionReview(
  val solutionSent: LocalDateTime,
  val solutionReviewed: LocalDateTime,
)

class MockTeacherStatistics : TeacherStatistics {
  private val teacherStats: MutableMap<Long, MutableList<SolutionReview>> = mutableMapOf()
  private var uncheckedSolutions = 0

  override fun recordNewSolution(solution: Solution) {
    uncheckedSolutions++
  }

  override fun recordAssessment(teacherId: Long, solution: Solution, timestamp: LocalDateTime) {
    teacherStats.getOrPut(teacherId) { mutableListOf() }
      .add(SolutionReview(solution.timestamp, timestamp))
    if (uncheckedSolutions > 0) {
      uncheckedSolutions--
    }
  }

  override fun getTeacherStats(teacherId: Long): TeacherStatsData? {
    val assessments = teacherStats[teacherId] ?: return null

    val totalAssessments = assessments.size
    val lastAssessment = assessments.maxByOrNull { it.solutionReviewed } ?: return null
    val firstAssessment = assessments.minByOrNull { it.solutionReviewed } ?: return null

    val averagePerDay =
      totalAssessments / (
        1 + ChronoUnit.DAYS.between(firstAssessment.solutionReviewed, LocalDateTime.now())
          .toDouble()
        )

    val averageCheckTime = assessments.sumOf {
      ChronoUnit.SECONDS.between(it.solutionSent, it.solutionReviewed).toDouble() / assessments.size
    }

    return TeacherStatsData(
      totalAssessments = totalAssessments,
      lastAssessmentTime = lastAssessment.solutionReviewed,
      averageAssessmentsPerDay = averagePerDay,
      averageCheckTimeSeconds = averageCheckTime,
    )
  }

  override fun getGlobalStats(): GlobalTeacherStats {
    val allTeacherStats = getAllTeachersStats().values

    val avgCheckTime = allTeacherStats
      .map { it.averageCheckTimeSeconds }
      .takeIf { it.isNotEmpty() }
      ?.average() ?: 0.0

    return GlobalTeacherStats(
      averageCheckTimeHours = avgCheckTime / 3600.0,
      totalUncheckedSolutions = uncheckedSolutions,
    )
  }

  override fun getAllTeachersStats(): Map<Long, TeacherStatsData> {
    return teacherStats.keys.associateWith { getTeacherStats(it) }
      .filterValues { it != null } as Map<Long, TeacherStatsData>
  }

  fun addMockFilling(teacherId: Long) {
    teacherStats.getOrPut(teacherId) { mutableListOf() }.add(SolutionReview(LocalDateTime.now().minusHours(2), LocalDateTime.now()))
  }
}
