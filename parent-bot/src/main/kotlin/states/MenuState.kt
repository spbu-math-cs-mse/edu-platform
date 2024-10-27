package states

import Student
import com.github.heheteam.samplebot.mockParents
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
    strictlyOn<MenuState> { state ->
        if (state.context.username == null) {
            return@strictlyOn null
        }
        val username = state.context.username!!.username
        if (!mockParents.containsKey(username)) {
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
                replyMarkup = Keyboards.menu(mockParents[username]!!.children),
            )

        when (val command = waitDataCallbackQuery().first().data) {
            Keyboards.petDog -> {
                bot.delete(stickerMessage)
                bot.delete(menuMessage)
                bot.sendSticker(state.context, Dialogues.heartSticker)
                bot.send(state.context, Dialogues.petDog())
                return@strictlyOn MenuState(state.context)
            }

            Keyboards.giveFeedback -> {
                bot.delete(menuMessage)
                return@strictlyOn GivingFeedbackState(state.context)
            }

            else -> {
                bot.delete(stickerMessage)
                bot.delete(menuMessage)
                return@strictlyOn ChildPerformanceState(state.context, Student(command))
            }
        }
    }
}
