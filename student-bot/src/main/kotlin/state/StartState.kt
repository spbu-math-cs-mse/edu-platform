package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnStartState(
  studentStorage: StudentStorage,
) {
  strictlyOn<StartState> { state ->
    bot.sendSticker(state.context, Dialogues.greetingSticker)
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val studentResolvedByTgId =
      studentStorage.resolveByTgId(state.context.id).get()?.id
    val studentId =
      studentResolvedByTgId ?: registerStudent(state.context, studentStorage)

    MenuState(state.context, studentId)
  }
}

private suspend fun BehaviourContext.registerStudent(
  tgUser: User,
  studentStorage: StudentStorage,
): StudentId {
  bot.send(tgUser, Dialogues.greetings())
  bot.send(tgUser, Dialogues.askFirstName())
  val firstName =
    waitTextMessageWithUser(tgUser.id).first().content.text

  bot.send(tgUser, Dialogues.askLastName(firstName))
  val lastName =
    waitTextMessageWithUser(tgUser.id).first().content.text

  val askGradeMessage =
    bot.send(
      tgUser,
      Dialogues.askGrade(firstName, lastName),
      replyMarkup = Keyboards.askGrade(),
    )

  // discard student class data
  waitDataCallbackQueryWithUser(tgUser.id).first().data
  val newStudent = studentStorage.createStudent()
  editMessageReplyMarkup(askGradeMessage, replyMarkup = null)
  return newStudent
}
