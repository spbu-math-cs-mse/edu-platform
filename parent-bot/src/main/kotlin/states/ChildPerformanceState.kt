package states

import Dialogues
import Keyboards
import com.github.heheteam.parentbot.MockGradeTable
import com.github.heheteam.parentbot.mockParents
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnChildPerformanceState(mockGradeTable: MockGradeTable) {
  strictlyOn<ChildPerformanceState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val username = state.context.username!!.username
    if (!mockParents.containsKey(username)) {
      return@strictlyOn StartState(state.context)
    }

    bot.sendSticker(state.context, Dialogues.nerdSticker)
    bot.send(
      state.context,
      Dialogues.childPerformance(mockGradeTable, state.child),
      replyMarkup = Keyboards.returnBack(),
    )

    waitDataCallbackQuery().first()

    MenuState(state.context)
  }
}
