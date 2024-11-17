package com.github.heheteam.studentbot.state

import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(
  core: StudentCore,
  isDeveloperRun: Boolean,
) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }

    val (firstName, lastName) = if (!isDeveloperRun) {
      bot.send(state.context, Dialogues.greetings())
      bot.send(state.context, Dialogues.askFirstName())

      val firstName = waitTextMessage().first().content.text
      bot.send(state.context, Dialogues.askLastName(firstName))

      val lastName = waitTextMessage().first().content.text
      firstName to lastName
    } else {
      "Яспер" to "Моглот"
    }

    bot.send(
      state.context,
      Dialogues.askGrade(firstName, lastName),
      replyMarkup = Keyboards.askGrade(),
    )
    if (!isDeveloperRun) {
      // if developer run, userId is preser
      core.userIdRegistry.setUserId(state.context.id)
      core.userId = core.userIdRegistry.getUserId(state.context.id)!!
    }

    // discard student class data
    waitDataCallbackQuery().first().data

    MenuState(state.context)
  }
} 
