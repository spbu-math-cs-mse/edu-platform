package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.map
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

data class QueryCourseForCheckingDeadlinesState(
  override val context: User,
  val studentId: StudentId,
) : NavigationBotStateWithHandlers<StudentApi>() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Выберите курс" }

  override fun menuState(): State = MenuState(context, studentId)

  override fun createKeyboard(service: StudentApi): MenuKeyboardData<State?> {
    val courses = service.getStudentCourses(studentId)
    val coursesPicker = createCoursePicker(courses)
    return coursesPicker.map { course ->
      if (course != null) {
        CheckDeadlinesState(context, studentId, course)
      } else null
    }
  }
}
