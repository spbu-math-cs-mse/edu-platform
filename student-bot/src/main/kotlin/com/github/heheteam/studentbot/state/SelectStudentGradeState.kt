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

class SelectStudentGradeState(
  override val context: User,
  val firstName: String,
  val lastName: String,
) : NavigationBotStateWithHandlers<StudentApi>() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Меню" }

  override fun createKeyboard(service: StudentApi): MenuKeyboardData<State?> {
    TODO("Not yet implemented")
  }

  override fun createKeyboardOrResult(
    service: StudentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> {
    val data = (1..11).map { "$it класс" to it } + listOf("Студент" to null)
    return buildColumnMenu(
        data.map { (label, grade) ->
          simpleButtonData(label) { ConfirmAndGoToQuestState(context, firstName, lastName, grade) }
        }
      )
      .ok()
  }

  override fun menuState(): State = this

  override fun defaultState(): State = menuState()
}
