package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class StartState(override val context: User) : BotState<TeacherId?, String, TeacherStorage> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    teacherStorage: TeacherStorage,
  ): TeacherId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    val teacherId: TeacherId? = teacherStorage.resolveByTgId(context.id).get()?.id
    if (teacherId != null) {
      return teacherId
    }
    bot.send(context, Dialogues.greetings() + Dialogues.askFirstName())
    val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
    bot.send(context, Dialogues.askLastName(firstName))
    val lastName = bot.waitTextMessageWithUser(context.id).first().content.text
    return teacherStorage.createTeacher(firstName, lastName, context.id.chatId.long)
  }

  override suspend fun computeNewState(
    service: TeacherStorage,
    teacherId: TeacherId?,
  ): Pair<BotState<*, *, *>, String> {
    if (teacherId == null) {
      return Pair(DeveloperStartState(context), Dialogues.devIdIsNotLong())
    }
    return Pair(MenuState(context, teacherId), Dialogues.greetings())
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherStorage,
    message: String,
  ) {
    bot.send(context, message)
  }
}
