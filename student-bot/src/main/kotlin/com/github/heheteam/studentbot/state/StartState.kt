package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class StartState(override val context: User) : BotState<StudentId?, String?, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi): StudentId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    return service.loginByTgId(context.id).get()?.id
  }

  override fun computeNewState(service: StudentApi, input: StudentId?): Pair<State, String?> =
    if (input != null) {
      MenuState(context, input) to Dialogues.greetings()
    } else {
      AskFirstNameState(context) to Dialogues.greetings()
    }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: String?) {
    if (response != null) {
      bot.send(context, response)
    }
  }
}
