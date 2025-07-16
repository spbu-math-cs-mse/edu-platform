package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.simpleButtonData
import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

data class SelectStudentParentState(override val context: User, val from: String?) :
  NavigationBotStateWithHandlers<StudentApi>() {
  override val introMessageContent: TextSourcesList = buildEntities {
    +"\uD83D\uDC36 Привет! Я Такса Дуся — умная собака, которая любит математику и приключения.\n"
  }

  override fun createKeyboard(service: StudentApi): MenuKeyboardData<State?> {
    TODO("Not yet implemented")
  }

  override fun createKeyboardOrResult(
    service: StudentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> =
    buildColumnMenu(
        simpleButtonData("\uD83D\uDC66 Я — ученик") { StudentStartState(context, null, from) },
        simpleButtonData("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67 Я — родитель") {
          ParentStartState(context, from)
        },
      )
      .ok()

  override fun menuState(): State = this

  override fun defaultState(): State = menuState()
}
