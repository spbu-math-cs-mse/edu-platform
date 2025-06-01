package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.map
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

data class QueryCourseForViewingScheduledMessagesState(
  override val context: User,
  val adminId: AdminId,
) : NavigationBotStateWithHandlers<AdminApi>() {
  override val introMessageContent: TextSourcesList = buildEntities {
    +"Выберите курс для просмотра запланированных сообщений:"
  }

  override fun menuState(): State = MenuState(context, adminId)

  override fun createKeyboard(service: AdminApi): MenuKeyboardData<State?> {
    val courses =
      service.getCourses().values.toList() // AdminApi::getCourses returns Map<String, Course>
    val coursesPicker = createCoursePicker(courses)
    return coursesPicker.map { course ->
      if (course != null) {
        QueryNumberOfRecentMessagesState(context, adminId, course.id)
      } else null
    }
  }
}
