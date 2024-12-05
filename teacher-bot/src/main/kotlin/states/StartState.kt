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
    var teacherId = teacherIdRegistry.getUserId(state.context.id)
    if (!isDeveloperRun && teacherId == null) {
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
      teacherId = teacherStorage.createTeacher()
      // TODO: put teacherId into teacherIdRegistry
      // TODO : put Teacher(teacherId) into teacherStorage
      return@strictlyOn MenuState(state.context, teacherId)
    } else if (isDeveloperRun) {
      bot.send(state.context, Dialogues.devAskForId())
      while (true) {
        val teacherIdFromText = waitTextMessage().first().content.text.toLongOrNull()?.let { TeacherId(it) }
        if (teacherIdFromText == null) {
          bot.send(state.context, Dialogues.devIdIsNotLong())
          continue
        }
        val teacher = teacherStorage.resolveTeacher(teacherIdFromText)
        if (teacher == null) {
          bot.send(state.context, Dialogues.devIdNotFound())
          continue
        }
        teacherId = teacherIdFromText
        break
      }
    }
    bot.send(
      state.context,
      Dialogues.greetings(),
    )
    MenuState(state.context, teacherId!!)
  }
}
