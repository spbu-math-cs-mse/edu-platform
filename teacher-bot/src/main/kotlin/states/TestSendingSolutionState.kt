package states

import Dialogues
import Keyboards
import Solution
import Problem
import com.github.heheteam.samplebot.mockTeachers
import com.github.heheteam.samplebot.mockSolutions
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnTestSendingSolutionState() {
    strictlyOn<TestSendingSolutionState> { state ->
        if (state.context.username == null) {
            return@strictlyOn null
        }
        val username = state.context.username!!.username
        if (!mockTeachers.containsKey(username)) {
            return@strictlyOn StartState(state.context)
        }

        val testSendMessage =
            bot.send(
                state.context,
                Dialogues.testSendSolution(),
                replyMarkup = Keyboards.returnBack(),
            )

        when (val response = flowOf(waitDataCallbackQuery(), waitTextMessage()).flattenMerge().first()) {
            is DataCallbackQuery -> {
                val command = response.data
                if (command == Keyboards.returnBack) {
                    delete(testSendMessage)
                }
            }

            is CommonMessage<*> -> {
                val solution = response.content.textContentOrNull()

                if (solution != null) {
                    mockSolutions.add(Solution(solution.text, Problem("")))

                    bot.sendSticker(
                        state.context,
                        Dialogues.okSticker,
                    )
                    bot.send(
                        state.context,
                        "Готово!",
                    )
                } else {
                    bot.send(
                        state.context,
                        "Ошибка, попробуйте ещё раз...",
                    )
                }
            }
        }
        MenuState(state.context)
    }
}
