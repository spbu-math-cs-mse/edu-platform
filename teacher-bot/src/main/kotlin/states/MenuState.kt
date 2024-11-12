package com.github.heheteam.teacherbot.states

import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState(core: TeacherCore) {
    strictlyOn<MenuState> { state ->
        if (state.context.username == null) {
            return@strictlyOn null
        }
        if (core.getUserId(state.context.id) == null) {
            return@strictlyOn StartState(state.context)
        }

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
                Keyboards.getSolution -> {
                    bot.delete(stickerMessage)
                    bot.delete(menuMessage)
                    return@strictlyOn GettingSolutionState(state.context)
                }
                Keyboards.viewStats -> {
                    val userId = core.getUserId(state.context.id)
                    if (userId != null) {
                        val stats = core.getTeacherStats(userId)
                        bot.send(
                            state.context,
                            """
                        📊 Ваша статистика проверок:
                        
                        Всего проверено: ${stats.totalAssessments}
                        Среднее число проверок в день: %.2f
                        ${stats.lastAssessmentTime?.let { "Последняя проверка: $it" } ?: "Нет проверок"}
                        """.trimIndent().format(stats.averageAssessmentsPerDay),
                            replyMarkup = Keyboards.returnBack()
                        )
                    }
                    return@strictlyOn MenuState(state.context)
                    }
            }
        }
        return@strictlyOn GettingSolutionState(state.context)
    }
}
