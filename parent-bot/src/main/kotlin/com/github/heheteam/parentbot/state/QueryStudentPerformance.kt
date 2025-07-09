package com.github.heheteam.parentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.createPickerWithBackButtonFromList
import com.github.heheteam.commonlib.util.map
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class QueryStudentPerformance(override val context: User, override val userId: ParentId) :
  NavigationBotStateWithHandlersAndUserId<ParentApi, ParentId>() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Выберите ребенка" }

  override fun createKeyboard(service: ParentApi): Result<MenuKeyboardData<State?>, FrontendError> =
    binding {
      val students = service.getChildrenOfParent(userId).bind()
      createPickerWithBackButtonFromList(students) { "${it.surname} ${it.name}" }
        .map { studentOrNull ->
          studentOrNull?.run { QueryCourseForStudentPerformance(context, userId, this.id) }
        }
    }

  override fun menuState(): State = Menu(context, userId)
}
