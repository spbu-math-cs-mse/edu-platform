package com.github.heheteam.adminbot.states.general

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

interface AdminHandleable : State {
  suspend fun handleAdmin(
    bot: BehaviourContext,
    service: AdminApi,
    initUpdateHandlers: (UpdateHandlersControllerDefault<out Any?>, context: User) -> Unit =
      { _, _ ->
      },
  ): State
}

abstract class AdminBotStateWithHandlers<In, Out> :
  BotStateWithHandlers<In, Out, AdminApi>, AdminHandleable {
  final override suspend fun handleAdmin(
    bot: BehaviourContext,
    service: AdminApi,
    initUpdateHandlers: (UpdateHandlersControllerDefault<out Any?>, User) -> Unit,
  ): State {
    return handle(bot, service, initUpdateHandlers)
  }
}

abstract class AdminNavigationBotStateWithHandlers :
  NavigationBotStateWithHandlers<AdminApi>(), AdminHandleable {
  final override suspend fun handleAdmin(
    bot: BehaviourContext,
    service: AdminApi,
    initUpdateHandlers: (UpdateHandlersControllerDefault<out Any?>, User) -> Unit,
  ): State {
    return handle(bot, service, initUpdateHandlers)
  }
}
