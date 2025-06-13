package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.toAdminId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.toResultOr
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class DeveloperStartState(override val context: User, val adminId: AdminId? = 1L.toAdminId()) :
  BotState<AdminId?, String, AdminApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: AdminApi): AdminId? {
    bot.sendSticker(context, Dialogues.greetingSticker)
    bot.send(context, Dialogues.devAskForId)
    return bot.waitTextMessageWithUser(context.id).first().content.text.toLongOrNull()?.toAdminId()
  }

  override suspend fun computeNewState(service: AdminApi, input: AdminId?): Pair<State, String> =
    binding {
        val adminId = input.toResultOr { Dialogues.devIdIsNotLong }.bind()
        //        service.loginById(adminId).mapError { Dialogues.devIdNotFound }.bind()
        //        service.updateTgId(adminId, context.id)
        Pair(MenuState(context, adminId), Dialogues.greetings)
      }
      .getOrElse { Pair(DeveloperStartState(context, adminId), it) }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: String) {
    bot.send(context, response)
  }
}
