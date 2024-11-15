package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.SolutionDistributor
import com.github.heheteam.commonlib.UserIdRegistry
import com.github.heheteam.commonlib.statistics.MockTeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatistics
import com.github.heheteam.commonlib.statistics.TeacherStatsData

class TeacherCore(
  private val solutionDistributor: SolutionDistributor,
  private val userIdRegistry: UserIdRegistry,
  private val teacherStatistics: TeacherStatistics = MockTeacherStatistics(),
) : UserIdRegistry by userIdRegistry,
  SolutionDistributor by solutionDistributor {
  fun getTeacherStats(teacherId: String): TeacherStatsData? {
    return teacherStatistics.getTeacherStats(teacherId)
  }
  fun getGlobalStats() = teacherStatistics.getGlobalStats()
}

var mockTgUsername: String = ""
