package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.api.GlobalTeacherStats
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.TeacherStatsData
import dev.inmo.tgbotapi.utils.mapNotNullValues
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MockTeacherStatistics : TeacherStatistics {
  private data class SolutionReview(
    val solutionSent: LocalDateTime,
    val solutionReviewed: LocalDateTime,
  )

  private val teacherStats: MutableMap<TeacherId, MutableList<SolutionReview>> = mutableMapOf()
  private var uncheckedSolutions = 0

  override fun recordNewSolution(solutionId: SolutionId) {
    uncheckedSolutions++
  }

  override fun recordAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    timestamp: LocalDateTime,
    solutionDistributor: SolutionDistributor,
  ) {
    val solution = solutionDistributor.resolveSolution(solutionId)
    teacherStats
      .getOrPut(teacherId) { mutableListOf() }
      .add(SolutionReview(solution.timestamp, timestamp))
    if (uncheckedSolutions > 0) {
      uncheckedSolutions--
    }
  }

  override fun getTeacherStats(teacherId: TeacherId): TeacherStatsData? {
    val assessments = teacherStats[teacherId] ?: return null

    val totalAssessments = assessments.size
    val lastAssessment = assessments.maxByOrNull { it.solutionReviewed } ?: return null
    val firstAssessment = assessments.minByOrNull { it.solutionReviewed } ?: return null

    val averagePerDay =
      totalAssessments / (
        1 +
          ChronoUnit.DAYS
            .between(firstAssessment.solutionReviewed, LocalDateTime.now())
            .toDouble()
        )

    val averageCheckTime =
      assessments.sumOf {
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

    val avgCheckTime =
      allTeacherStats
        .map { it.averageCheckTimeSeconds }
        .takeIf { it.isNotEmpty() }
        ?.average() ?: 0.0

    return GlobalTeacherStats(
      averageCheckTimeHours = avgCheckTime / 3600.0,
      totalUncheckedSolutions = uncheckedSolutions,
    )
  }

  override fun getAllTeachersStats(): Map<TeacherId, TeacherStatsData> =
    teacherStats.keys
      .associateWith { getTeacherStats(it) }
      .mapNotNullValues()

  fun addMockFilling(teacherId: TeacherId) {
    teacherStats.getOrPut(teacherId) { mutableListOf() }.add(SolutionReview(LocalDateTime.now().minusHours(2), LocalDateTime.now()))
  }
}
