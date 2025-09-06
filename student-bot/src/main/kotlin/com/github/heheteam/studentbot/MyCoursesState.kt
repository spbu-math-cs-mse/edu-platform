package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.map
import com.github.heheteam.studentbot.state.MenuState
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class MyCoursesState(
  override val context: User,
  override val userId: StudentId,
  val courses: List<Course>,
) : NavigationBotStateWithHandlersAndStudentId<StudentApi>() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Выберите курс" }

  override fun createKeyboard(
    service: StudentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> = binding {
    val coursesPicker = createCoursePicker(courses)
    coursesPicker.map { course ->
      if (course != null) {
        service.saveSelectedCourse(userId, course.id)
        MenuState(context, userId)
      } else null
    }
  }

  override fun menuState(): State = MenuState(context, userId)
}
