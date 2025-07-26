package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.simpleButtonData
import com.github.heheteam.studentbot.state.parent.ParentMenuState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class SelectParentGradeState(
  override val context: User,
  val firstName: String,
  val lastName: String,
  private val from: String?,
) : NavigationBotStateWithHandlers<ParentApi>() {
  override val introMessageContent: TextSourcesList = buildEntities {
    +"А в каком классе учится ваш ребёнок? "
  }

  override fun createKeyboard(service: ParentApi): MenuKeyboardData<State?> {
    TODO("Not yet implemented")
  }

  override fun createKeyboardOrResult(
    service: ParentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> = binding {
    val parent = service.createParent(firstName, lastName, context.id.chatId, from).bind()
    val data = (1..11).map { "$it класс" to it } + listOf("Студент" to null)
    buildColumnMenu(
      data.map { (label, grade) -> simpleButtonData(label) { ParentMenuState(context, parent.id) } }
    )
  }

  override fun menuState(): State = this

  override fun defaultState(): State = menuState()
}
