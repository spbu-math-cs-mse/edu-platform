package com.github.heheteam.commonlib.api

import com.github.michaelbull.result.Result
import java.time.LocalDateTime

interface TeacherStatistics {
  fun recordAssessment(
    teacherId: TeacherId,
    solutionId: SolutionId,
    timestamp: LocalDateTime,
    solutionDistributor: SolutionDistributor,
  )

  fun recordNewSolution(solutionId: SolutionId)

  fun resolveTeacherStats(teacherId: TeacherId): Result<TeacherStatsData, ResolveError<TeacherId>>

  fun getAllTeachersStats(): Map<TeacherId, TeacherStatsData>

  fun getGlobalStats(): GlobalTeacherStats
}

data class TeacherStatsData(
  val totalAssessments: Int,
  val lastAssessmentTime: LocalDateTime,
  val averageAssessmentsPerDay: Double,
  val averageCheckTimeSeconds: Double,
)

data class GlobalTeacherStats(val averageCheckTimeHours: Double, val totalUncheckedSolutions: Int)
