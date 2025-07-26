package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User

open class DefaultErrorState<ApiService : CommonUserApi<UserId>, UserId : CommonUserId>(
  override val context: User,
  override val userId: UserId,
  val nextState: State,
) : QuestState<ApiService, UserId>() {
  override suspend fun BotContext.run(service: ApiService) {
    val buttons = listOf("✅ Конечно!", "\uD83D\uDD19 Назад")
    send(
        "\uD83D\uDD12 Похоже, этот ответ не совсем верный. Попробуем еще раз?",
        replyMarkup = verticalKeyboard(buttons),
      )
      .also { messagesWithKeyboard.add(it) }
    addDataCallbackHandler { callbackQuery ->
      when (callbackQuery.data) {
        buttons[0] -> NewState(nextState)
        buttons[1] -> NewState(menuState())
        else -> Unhandled
      }
    }
  }
}

class DefaultErrorStateStudent(context: User, userId: StudentId, nextState: State) :
  DefaultErrorState<StudentApi, StudentId>(context, userId, nextState)

class DefaultErrorStateParent(context: User, userId: ParentId, nextState: State) :
  DefaultErrorState<ParentApi, ParentId>(context, userId, nextState)
