package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.StudentApi
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class StartState(override val context: User) : BotState<StudentId?, String, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi): StudentId {
    bot.sendSticker(context, Dialogues.greetingSticker)
    val studentId: StudentId? = service.loginByTgId(context.id).get()?.id
    if (studentId != null) {
      return studentId
    }
    bot.send(context, Dialogues.greetings() + Dialogues.askFirstName())
    val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
    bot.send(context, Dialogues.askLastName(firstName))
    val lastName = bot.waitTextMessageWithUser(context.id).first().content.text
    return service.createStudent(firstName, lastName, context.id.chatId.long)
  }

  override fun computeNewState(service: StudentApi, input: StudentId?): Pair<State, String> {
    val studentId = input ?: return Pair(DeveloperStartState(context), Dialogues.devIdIsNotLong())
    return Pair(MenuState(context, studentId), Dialogues.greetings())
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: String) {
    bot.send(context, response)
  }
}
