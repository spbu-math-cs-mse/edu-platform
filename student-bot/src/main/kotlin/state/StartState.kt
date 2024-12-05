package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentIdRegistry
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(studentIdRegistry: StudentIdRegistry, studentStorage: StudentStorage, isDeveloperRun: Boolean = false) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }

    var studentId = studentIdRegistry.getUserId(state.context.id)
    if (!isDeveloperRun && studentId == null) {
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
      studentId = studentStorage.createStudent()
      // TODO: put studentId into studentIdRegistry
      // TODO : put Student(studentId, ...) into studentStorage
      editMessageReplyMarkup(askGradeMessage, replyMarkup = null)
    } else if (isDeveloperRun) {
      bot.send(state.context, Dialogues.devAskForId())
      while (true) {
        val studentIdFromText = waitTextMessage().first().content.text.toLongOrNull()?.let { StudentId(it) }
        if (studentIdFromText == null) {
          bot.send(state.context, Dialogues.devIdIsNotLong())
          continue
        }
        val student = studentStorage.resolveStudent(studentIdFromText)
        if (student == null) {
          bot.send(state.context, Dialogues.devIdNotFound())
          continue
        }
        studentId = studentIdFromText
        break
      }
    }
    MenuState(state.context, studentId!!)
  }
}
