package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.studentbot.Keyboards
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

data class RequestChallengeState(
  override val context: User,
  override val userId: StudentId,
  private val course: Course,
) : SimpleStudentState() {
  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: StudentApi) {
    send("Вы точно хотите запросить доступ к челленджу?", replyMarkup = Keyboards.confirm())
      .deleteLater()

    addDataCallbackHandler { callback ->
      if (callback.data == Keyboards.YES) {
        send("Отправляю запрос на доступ к челленджу...").deleteLater()
        service.requestChallengeAccess(userId, course.id)
        send("Запрос на доступ к челленджу отправлен")
      }

      NewState(MenuState(context, userId))
    }
  }
}
