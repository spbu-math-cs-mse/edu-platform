package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toStackedString
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.map
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

data class QueryCourseForCheckingGradesState(
  override val context: User,
  override val userId: StudentId,
) : NavigationBotStateWithHandlersAndStudentId<StudentApi>() {
  override val introMessageContent: TextSourcesList = buildEntities { +"Выберите курс" }

  override fun menuState(): State = MenuState(context, userId)

  override fun createKeyboard(
    service: StudentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> = binding {
    val courses = service.getStudentCourses(userId).bind()
    val coursesPicker = createCoursePicker(courses)
    coursesPicker.map { course ->
      if (course != null) {
        val assignments = service.getCourseAssignments(course.id)
        assignments.mapBoth(
          success = { QueryAssignmentForCheckingGradesState(context, userId, course.id, it) },
          failure = {
            logger.error("service.getCourseAssignments failed: ${it.toStackedString()}")
            null
          },
        )
      } else null
    }
  }
}
