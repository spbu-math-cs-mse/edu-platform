package com.github.heheteam.parentbot.state

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.InformationState
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.map
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.map
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class QueryCourseForStudentPerformance(
  override val context: User,
  override val userId: ParentId,
  val studentId: StudentId,
) : NavigationBotStateWithHandlersAndUserId<ParentApi, ParentId>() {
  override val introMessageContent: TextSourcesList
    get() = buildEntities { +"Выберите курс для просмотра его успеваемости" }

  override fun createKeyboard(service: ParentApi): Result<MenuKeyboardData<State?>, FrontendError> =
    binding {
      val result = service.getStudentCourses(studentId).bind()
      createCoursePicker(result).map { course ->
        if (course != null) {
          InformationState<ParentApi, ParentId>(
            context,
            userId,
            {
              service.getStudentPerformance(studentId, course.id).map { grades ->
                formatGrades(grades)
              }
            },
            menuState(),
          )
        } else null
      }
    }

  override fun menuState(): State = Menu(context, userId)
}

private fun formatGrades(grades: Map<Problem, Grade>): TextWithMediaAttachments =
  TextWithMediaAttachments(
    buildEntities { grades.forEach { (problem, grade) -> +"${problem.number} -> $grade\n" } }
  )
