package states

import Dialogues
import Keyboards
import Parent
import com.github.heheteam.parentbot.mockParents
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState() {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val username = state.context.username!!.username
    if (!mockParents.containsKey(username)) {
      bot.send(
        state.context,
        Dialogues.greetings() + Dialogues.askFirstName(),
      )
      val firstName = waitTextMessage().first().content.text
      bot.send(
        state.context,
        Dialogues.askLastName(firstName),
      )
      val lastName = waitTextMessage().first().content.text
      bot.send(
        state.context,
        Dialogues.askGrade(firstName, lastName),
        replyMarkup = Keyboards.askGrade(),
      )
      val grade = waitDataCallbackQuery().first().data
      if (grade == "Родитель") {
        mockParents[username] = Parent((mockParents.size + 1).toString(), listOf())
      }
      return@strictlyOn MenuState(state.context)
    }
    bot.send(
      state.context,
      Dialogues.greetings(),
    )
    MenuState(state.context)
  }
}
