package com.github.heheteam.teacherbot.states

import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(core: TeacherCore) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (core.getUserId(state.context.id.toString()) == null) {
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
      bot.send(
        state.context,
        Dialogues.askIdentifier(),
      )
      val identifier = waitTextMessage().first().content.text
      if (grade == "Другое") {
        core.setUserId(state.context.id.toString(), identifier)
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
