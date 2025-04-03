package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.commonlib.api.TeacherApi
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class StartState(override val context: User) : BotState<TeacherId?, String, TeacherApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherApi): TeacherId {
    val teacherApi = service
    bot.sendSticker(context, Dialogues.greetingSticker)
    val teacherId: TeacherId? = teacherApi.tryLoginByTgId(context.id).get()?.id
    if (teacherId != null) {
      return teacherId
    }
    bot.send(context, Dialogues.greetings() + Dialogues.askFirstName)
    val firstName = bot.waitTextMessageWithUser(context.id).first().content.text
    bot.send(context, Dialogues.askLastName(firstName))
    val lastName = bot.waitTextMessageWithUser(context.id).first().content.text
    return teacherApi.createTeacher(firstName, lastName, context.id.chatId.long)
  }

  override fun computeNewState(service: TeacherApi, input: TeacherId?): Pair<State, String> {
    val teacherId = input ?: return Pair(DeveloperStartState(context), Dialogues.devIdIsNotLong)
    return Pair(MenuState(context, teacherId), Dialogues.greetings())
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: TeacherApi, response: String) {
    bot.send(context, response)
  }
}
