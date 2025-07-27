package com.github.heheteam.adminbot.states.scheduled

import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.adminbot.states.general.AdminNavigationBotStateWithHandlers
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.simpleButtonData
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class ScheduledMessagesMenu(override val context: User, val adminId: AdminId) :
  AdminNavigationBotStateWithHandlers() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Меню рассылок" }

  override fun createKeyboard(service: AdminApi): MenuKeyboardData<State> {
    return buildColumnMenu(
      simpleButtonData("Отправить") { AddScheduledMessageStartState(context, adminId) },
      simpleButtonData("Посмотреть") { QueryNumberOfRecentMessagesState(context, adminId) },
      simpleButtonData("Удалить") { QueryMessageIdForDeletionState(context, adminId) },
      simpleButtonData("Назад (в меню)") { MenuState(context, adminId) },
    )
  }

  override fun menuState(): State = MenuState(context, adminId)

  override fun defaultState(): State = MenuState(context, adminId)
}
