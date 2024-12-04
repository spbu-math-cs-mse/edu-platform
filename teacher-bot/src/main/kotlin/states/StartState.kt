package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherIdRegistry
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(
  teacherIdRegistry: TeacherIdRegistry,
  teacherStorage: TeacherStorage,
  isDeveloperRun: Boolean = false,
) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (!isDeveloperRun && teacherIdRegistry.getUserId(state.context.id) == null) {
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
        val teacherId = waitTextMessage().first().content.text.toLongOrNull()?.let { TeacherId(it) }
        if (teacherId == null) {
          bot.send(state.context, Dialogues.devIdIsNotLong())
          continue
        }
        val teacher = teacherStorage.resolveTeacher(teacherId)
        if (teacher == null) {
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
