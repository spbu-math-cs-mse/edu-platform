package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.GlobalTeacherStats
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.TeacherStatsData
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.teacherbot.Keyboards
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

data class Statistics(val teacherStats: TeacherStatsData, val globalStats: GlobalTeacherStats)

class SendStatisticInfoState(override val context: User, val teacherId: TeacherId) :
  BotState<Unit, Statistics?, TeacherStatistics> {
  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherStatistics) {}

  override suspend fun computeNewState(
    teacherStatistics: TeacherStatistics,
    input: Unit,
  ): Pair<BotState<*, *, *>, Statistics?> {
    val teacherStats: TeacherStatsData? = teacherStatistics.resolveTeacherStats(teacherId).get()
    val globalStats = teacherStatistics.getGlobalStats()
    val stats =
      if (teacherStats != null) {
        Statistics(teacherStats, globalStats)
      } else {
        null
      }
    return Pair(MenuState(context, teacherId), stats)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherStatistics,
    stats: Statistics?,
  ) {
    if (stats == null) return
    bot.send(
      context,
      """
              📊 Ваша статистика проверок:
              
              Всего проверено: ${stats.teacherStats.totalAssessments}
              Среднее число проверок в день: %.2f
              ${stats.teacherStats.lastAssessmentTime.let { "Последняя проверка: $it" } ?: "Нет проверок"}
              ${
        stats.teacherStats.averageCheckTimeSeconds.let {
          "Среднее время на проверку: %.1f часов".format(
            it / 60 / 60
          )
        }
      }
              
              📈 Общая статистика:
              Среднее время проверки: %.1f часов
              Всего непроверенных работ: %d
    """
        .trimIndent()
        .format(
          stats.teacherStats.averageAssessmentsPerDay,
          stats.globalStats.averageCheckTimeHours,
          stats.globalStats.totalUncheckedSolutions,
        ),
      replyMarkup = Keyboards.returnBack(),
    )
  }
}
