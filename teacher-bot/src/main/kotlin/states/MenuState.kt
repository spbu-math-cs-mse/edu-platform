package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherIdRegistry
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState(
  userIdRegistry: TeacherIdRegistry,
  core: TeacherCore,
) {
  strictlyOn<MenuState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val userId = userIdRegistry.getUserId(state.context.id) ?: return@strictlyOn StartState(state.context)

    val stickerMessage =
      bot.sendSticker(
        state.context,
        Dialogues.typingSticker,
      )

    val menuMessage =
      bot.send(
        state.context,
        Dialogues.menu(),
        replyMarkup = Keyboards.menu(),
      )

    while (true) {
      val callbackData = waitDataCallbackQuery().first().data
      when (callbackData) {
        Keyboards.checkGrades -> {
          bot.delete(menuMessage)
          return@strictlyOn CheckGradesState(state.context)
        }

        Keyboards.getSolution -> {
          bot.delete(stickerMessage)
          bot.delete(menuMessage)
          return@strictlyOn GettingSolutionState(state.context)
        }

        Keyboards.viewStats -> {
          val stats = core.getTeacherStats(userId)
          if (stats != null) {
            val globalStats = core.getGlobalStats()

            bot.send(
              state.context,
              """
              📊 Ваша статистика проверок:
              
              Всего проверено: ${stats.totalAssessments}
              Среднее число проверок в день: %.2f
              ${stats.lastAssessmentTime.let { "Последняя проверка: $it" } ?: "Нет проверок"}
              ${stats.averageCheckTimeSeconds.let { "Среднее время на проверку: %.1f часов".format(it / 60 / 60) } ?: ""}
              
              📈 Общая статистика:
              Среднее время проверки: %.1f часов
              Всего непроверенных работ: %d
              """.trimIndent().format(
                stats.averageAssessmentsPerDay,
                globalStats.averageCheckTimeHours,
                globalStats.totalUncheckedSolutions,
              ),
              replyMarkup = Keyboards.returnBack(),
            )
          }
          return@strictlyOn MenuState(state.context)
        }
      }
    }
    return@strictlyOn GettingSolutionState(state.context)
  }
}
