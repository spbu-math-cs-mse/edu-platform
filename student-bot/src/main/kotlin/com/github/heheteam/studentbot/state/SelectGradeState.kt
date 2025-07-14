package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.simpleButtonData
import com.github.heheteam.studentbot.ConfirmAndGoToQuestState
import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class SelectGradeState(override val context: User, val firstName: String, val lastName: String) :
  NavigationBotStateWithHandlers<StudentApi>() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Меню" }

  override fun createKeyboard(service: StudentApi): MenuKeyboardData<State?> {
    TODO("Not yet implemented")
  }

  override fun createKeyboardOrResult(
    service: StudentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> =
    buildColumnMenu(
        simpleButtonData("4 класс") { ConfirmAndGoToQuestState(context, firstName, lastName, 4) },
        simpleButtonData("5 класс") { ConfirmAndGoToQuestState(context, firstName, lastName, 4) },
        simpleButtonData("6 класс") { ConfirmAndGoToQuestState(context, firstName, lastName, 4) },
      )
      .ok()

  override fun menuState(): State = this

  override fun defaultState(): State = menuState()
}
