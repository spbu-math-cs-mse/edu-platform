package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.studentbot.Keyboards
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.datetime.toKotlinLocalDateTime

data class RescheduleDeadlinesState(override val context: User, override val userId: StudentId) :
  SimpleStudentState() {
  override suspend fun BotContext.run(service: StudentApi) {
    send("Вы точно хотите попросить перенести дедлайны?", replyMarkup = Keyboards.confirm())
      .deleteLater()
    addDataCallbackHandler { callback ->
      if (callback.data == Keyboards.YES) {
        send("Отправляю запрос на перенос дедлайнов...").deleteLater()
        val newDeadline =
          java.time.ZonedDateTime.now().plusMinutes(2).toLocalDateTime().toKotlinLocalDateTime()

        service
          .requestReschedulingDeadlines(userId, newDeadline)
          .mapBoth(
            success = { send("Запрос на перенос дедлайнов отправлен") },
            failure = {
              send(
                "Случилась ошибка! Не волнуйтесь, разработчики уже в пути решения этой проблемы.\n" +
                  "Ошибка: ${it.shortDescription}"
              )
            },
          )
      }

      NewState(MenuState(context, userId))
    }
  }

  override fun defaultState(): State = MenuState(context, userId)
}
