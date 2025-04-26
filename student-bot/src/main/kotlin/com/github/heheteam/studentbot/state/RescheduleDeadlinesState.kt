package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.toKotlinLocalDateTime

data class RescheduleDeadlinesState(override val context: User, val userId: StudentId) :
  BotState<Unit, Unit, StudentApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: StudentApi) {
    bot.send(context, "Отправляю запрос на перенос дедлайнов...")
  }

  override fun computeNewState(service: StudentApi, input: Unit): Pair<State, Unit> {
    val newDeadline =
      java.time.ZonedDateTime.now().plusMinutes(2).toLocalDateTime().toKotlinLocalDateTime()
    service.requestReschedulingDeadlines(userId, newDeadline)

    return MenuState(context, userId) to Unit
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) {
    bot.send(context, "Запрос на перенос дедлайнов отправлен")
  }
}
