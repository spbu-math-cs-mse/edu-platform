package com.github.heheteam.parentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class Menu(override val context: User, override val userId: ParentId) :
  NavigationBotStateWithHandlersAndUserId<ParentApi, ParentId>() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Меню" }

  override fun createKeyboard(service: ParentApi): Result<MenuKeyboardData<State?>, FrontendError> =
    buildColumnMenu(
        ButtonData("Добавить ребенка", "1") { AddChildById(context, userId) },
        ButtonData("Посмотреть успеваемость ребенка", "2") {
          QueryStudentPerformance(context, userId)
        },
      )
      .ok()

  override fun menuState(): State = this
}
