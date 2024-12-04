import com.github.heheteam.commonlib.api.ParentIdRegistry
import com.github.heheteam.parentbot.states.BotState
import com.github.heheteam.parentbot.states.MenuState
import com.github.heheteam.parentbot.states.StartState
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(userIdRegistry: ParentIdRegistry, isDeveloperRun: Boolean = false) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }

    if (!isDeveloperRun) {
      bot.send(state.context, Dialogues.greetings())

      bot.send(state.context, Dialogues.askFirstName())
      val firstName = waitTextMessage().first().content.text

      bot.send(state.context, Dialogues.askLastName(firstName))
      val lastName = waitTextMessage().first().content.text

      val askGradeMessage =
        bot.send(
          state.context,
          Dialogues.askGrade(firstName, lastName),
          replyMarkup = Keyboards.askGrade(),
        )

      // discard student class data
      waitDataCallbackQuery().first().data
      editMessageReplyMarkup(askGradeMessage, replyMarkup = null)
    } else {
      bot.send(state.context, Dialogues.devAskForId())
      while (true) {
        val id = waitTextMessage().first().content.text.toLongOrNull()?.let { userIdRegistry.getUserId(UserId(RawChatId(it))) }
        if (id == null) {
          bot.send(state.context, Dialogues.devIdNotFound())
          continue
        }
        break
      }
    }
    MenuState(state.context)
  }
}
