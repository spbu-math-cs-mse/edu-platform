package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(isDeveloperRun: Boolean) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }

    val (firstName, lastName) =
      if (!isDeveloperRun) {
        bot.send(state.context, Dialogues.greetings())
        bot.send(state.context, Dialogues.askFirstName())

        val firstName = waitTextMessageWithUser(state.context.id).first().content.text
        bot.send(state.context, Dialogues.askLastName(firstName))

        val lastName = waitTextMessageWithUser(state.context.id).first().content.text
        firstName to lastName
      } else {
        "Яспер" to "Моглот"
      }

    val askGradeMessage =
      bot.send(
        state.context,
        Dialogues.askGrade(firstName, lastName),
        replyMarkup = Keyboards.askGrade(),
      )

    // discard student class data
    waitDataCallbackQueryWithUser(state.context.id).first().data
    editMessageReplyMarkup(askGradeMessage, replyMarkup = null)
    MenuState(state.context)
  }
}
