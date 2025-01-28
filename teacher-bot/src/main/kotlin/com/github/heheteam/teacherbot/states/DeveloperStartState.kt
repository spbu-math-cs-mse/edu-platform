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

class DeveloperStartState(override val context: User) :
  BotState<TeacherId?, String, TeacherStorage> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    teacherStorage: TeacherStorage,
  ): TeacherId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    bot.send(context, Dialogues.devAskForId())
    val teacherIdFromText =
      bot.waitTextMessageWithUser(context.id).first().content.text.toLongOrNull()?.let {
        TeacherId(it)
      }
    return teacherIdFromText
  }

  override suspend fun computeNewState(
    teacherStorage: TeacherStorage,
    teacherId: TeacherId?,
  ): Pair<BotState<*, *, *>, String> {
    if (teacherId == null) {
      return Pair(DeveloperStartState(context), Dialogues.devIdIsNotLong())
    }
    teacherStorage.resolveTeacher(teacherId).get()
      ?: return Pair(DeveloperStartState(context), Dialogues.devIdNotFound())
    return Pair(MenuState(context, teacherId), Dialogues.greetings())
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherStorage,
    response: String,
  ) {
    bot.send(context, response)
  }
}
