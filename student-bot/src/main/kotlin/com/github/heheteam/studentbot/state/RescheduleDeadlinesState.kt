package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.Keyboards
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import kotlinx.datetime.toKotlinLocalDateTime

data class RescheduleDeadlinesState(override val context: User, override val userId: StudentId) :
  BotStateWithHandlersAndStudentId<Unit, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) {
    sentMessages.forEach { bot.delete(it) }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Unit, NumberedError>,
  ): Result<Unit, NumberedError> = coroutineBinding {
    bot
      .send(
        context,
        "Вы точно хотите попросить перенести дедлайны?",
        replyMarkup = Keyboards.confirm(),
      )
      .also { sentMessages.add(it) }

    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == Keyboards.YES) {
        bot.send(context, "Отправляю запрос на перенос дедлайнов...").also { sentMessages.add(it) }
        val newDeadline =
          java.time.ZonedDateTime.now().plusMinutes(2).toLocalDateTime().toKotlinLocalDateTime()

        service
          .requestReschedulingDeadlines(userId, newDeadline)
          .mapBoth(
            success = { bot.send(context, "Запрос на перенос дедлайнов отправлен") },
            failure = {
              bot.send(
                context,
                "Случилась ошибка! Не волнуйтесь, разработчики уже в пути решения этой проблемы.\n" +
                  "Ошибка: ${it.shortDescription}",
              )
            },
          )
      }

      NewState(MenuState(context, userId))
    }
  }

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) =
    Unit

  override suspend fun computeNewState(service: StudentApi, input: Unit): Pair<State, Unit> =
    MenuState(context, userId) to Unit
}
