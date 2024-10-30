package states

import Dialogues.noSolutionsToCheck
import Keyboards
import com.github.heheteam.samplebot.mockSolutions
import com.github.heheteam.samplebot.mockTeachers
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGettingSolutionState() {
    strictlyOn<GettingSolutionState> { state ->
        if (state.context.username == null) {
            return@strictlyOn null
        }
        val username = state.context.username!!.username
        if (!mockTeachers.containsKey(username)) {
            return@strictlyOn StartState(state.context)
        }

        if (mockSolutions.isEmpty()) {
            bot.send(
                state.context,
                noSolutionsToCheck(),
            )
        } else {
            val getSolution =
                bot.send(
                    state.context,
                    mockSolutions.random().id,
                    replyMarkup = Keyboards.solutionMenu(),
                )

            when (val response = flowOf(waitDataCallbackQuery(), waitTextMessage()).flattenMerge().first()) {
                is DataCallbackQuery -> {
                    val command = response.data
                    if (command == Keyboards.returnBack) {
                        delete(getSolution)
                    }
                }
            }
        }
        MenuState(state.context)
    }
}
