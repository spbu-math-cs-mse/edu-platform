package states

import Dialogues
import Keyboards
import com.github.heheteam.samplebot.mockTeachers
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
    if (!mockTeachers.containsKey(username)) {
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

    when (val command = waitDataCallbackQuery().first().data) {
      Keyboards.testSendSolution -> {
        bot.delete(menuMessage)
        return@strictlyOn TestSendingSolutionState(state.context)
      }

      else -> {
        bot.delete(menuMessage)
        return@strictlyOn GettingSolutionState(state.context)
      }
    }
  }
}
