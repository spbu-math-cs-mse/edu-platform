package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext

abstract class SimpleTeacherState : SimpleState<TeacherApi, TeacherId>() {
  override fun defaultState(): State = MenuState(context, userId)

  /**
   * In teacher state, the exception will be handled at the top level, so we ignore the monadic
   * wrapper.
   */
  final override suspend fun intro(
    bot: BehaviourContext,
    service: TeacherApi,
    updateHandlersController: UpdateHandlerManager<Unit>,
  ): Result<Unit, FrontendError> =
    BotContext(bot, context, updateHandlersController).run(service).ok()
}
