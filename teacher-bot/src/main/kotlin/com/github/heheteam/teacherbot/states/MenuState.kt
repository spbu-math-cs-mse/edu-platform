package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStatsData
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState(core: TeacherCore) {
  strictlyOn<MenuState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }

    val teacherId = state.teacherId

    val stickerMessage = bot.sendSticker(state.context, Dialogues.typingSticker)

    val menuMessage = bot.send(state.context, Dialogues.menu(), replyMarkup = Keyboards.menu())

    val dataCallbackResponseFlow =
      waitDataCallbackQueryWithUser(state.context.id).map { callback ->
        handleDataCallback(callback, state, core, teacherId)
      }
    val texts =
      waitTextMessageWithUser(state.context.id).map { t -> handleTextMessage(t, state.context) }
    val nextState = merge(dataCallbackResponseFlow, texts).firstNotNull()
    deleteMessage(stickerMessage)
    deleteMessage(menuMessage)
    nextState
  }
}

private suspend fun BehaviourContext.handleTextMessage(
  t: CommonMessage<TextContent>,
  user: User,
): PresetTeacherState? {
  val re = Regex("/setid ([0-9]+)")
  val match = re.matchEntire(t.content.text)
  return if (match != null) {
    val newIdStr = match.groups[1]?.value ?: return null
    val newId =
      newIdStr.toLongOrNull()
        ?: run {
          logger.error("input id $newIdStr is not long!")
          return null
        }
    PresetTeacherState(user, newId.toTeacherId())
  } else {
    bot.sendMessage(user.id, "Unrecognized command")
    null
  }
}

private suspend fun BehaviourContext.handleDataCallback(
  callback: DataCallbackQuery,
  state: MenuState,
  core: TeacherCore,
  teacherId: TeacherId,
): BotState {
  val callbackData = callback.data
  val nextState =
    when (callbackData) {
      Keyboards.checkGrades -> {
        CheckGradesState(state.context, state.teacherId)
      }

      Keyboards.getSolution -> {
        GettingSolutionState(state.context, state.teacherId)
      }

      Keyboards.viewStats -> {
        val stats = core.getTeacherStats(teacherId)
        if (stats != null) {
          sendStatisticsInfo(core, state, stats)
        }
        MenuState(state.context, state.teacherId)
      }

      else -> null
    }
  return nextState ?: GettingSolutionState(state.context, state.teacherId)
}

private suspend fun BehaviourContext.sendStatisticsInfo(
  core: TeacherCore,
  state: MenuState,
  stats: TeacherStatsData,
) {
  val globalStats = core.getGlobalStats()

  bot.send(
    state.context,
    """
              📊 Ваша статистика проверок:
              
              Всего проверено: ${stats.totalAssessments}
              Среднее число проверок в день: %.2f
              ${stats.lastAssessmentTime.let { "Последняя проверка: $it" } ?: "Нет проверок"}
              ${
      stats.averageCheckTimeSeconds.let {
        "Среднее время на проверку: %.1f часов".format(
          it / 60 / 60
        )
      } ?: ""
    }
              
              📈 Общая статистика:
              Среднее время проверки: %.1f часов
              Всего непроверенных работ: %d
    """
      .trimIndent()
      .format(
        stats.averageAssessmentsPerDay,
        globalStats.averageCheckTimeHours,
        globalStats.totalUncheckedSolutions,
      ),
    replyMarkup = Keyboards.returnBack(),
  )
}
