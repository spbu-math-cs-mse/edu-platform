package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherIdRegistry
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(userIdRegistry: TeacherIdRegistry, isDeveloperRun: Boolean = false) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (!isDeveloperRun && userIdRegistry.getUserId(state.context.id) == null) {
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
      waitDataCallbackQuery().first().data // discard class
      return@strictlyOn MenuState(state.context)
    } else if (isDeveloperRun) {
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
    bot.send(
      state.context,
      Dialogues.greetings(),
    )
    MenuState(state.context)
  }
}
