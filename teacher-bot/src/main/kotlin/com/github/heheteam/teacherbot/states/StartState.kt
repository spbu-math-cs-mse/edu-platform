package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.teacherbot.Dialogues
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StartState(override val context: User) : BotState<TeacherId?, String?, TeacherApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherApi): TeacherId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    return service.loginByTgId(context.id).get()?.id
  }

  override suspend fun computeNewState(service: TeacherApi, input: TeacherId?): Pair<State, String?> =
    if (input != null) {
      MenuState(context, input) to Dialogues.greetings
    } else {
      AskFirstNameState(context) to Dialogues.greetings
    }

  override suspend fun sendResponse(bot: BehaviourContext, service: TeacherApi, response: String?) {
    if (response != null) {
      bot.send(context, response)
    }
  }
}
