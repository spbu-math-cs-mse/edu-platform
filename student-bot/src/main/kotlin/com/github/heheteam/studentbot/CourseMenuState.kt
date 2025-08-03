package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.simpleButtonData
import com.github.heheteam.studentbot.state.CheckDeadlinesState
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.QueryAssignmentForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryProblemForSubmissionSendingState
import com.github.heheteam.studentbot.state.RescheduleDeadlinesState
import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class CourseMenuState(
  override val context: User,
  override val userId: StudentId,
  val course: Course,
) : NavigationBotStateWithHandlersAndStudentId<StudentApi>() {
  override val introMessageContent: TextSourcesList
    get() = buildEntities { +"Меню курса" }

  override fun createKeyboard(
    service: StudentApi
  ): Result<MenuKeyboardData<State?>, FrontendError> = binding {
    buildColumnMenu(
      simpleButtonData("Отправить решение") {
        QueryProblemForSubmissionSendingState(context, userId, course.id)
      },
      simpleButtonData("Посмотреть успеваемость", { viewGradesNextState(service) }),
      simpleButtonData("Посмотреть дедлайны", { CheckDeadlinesState(context, userId, course) }),
      simpleButtonData("Попросить дорешку", { RescheduleDeadlinesState(context, userId) }),
      simpleButtonData("Челлендж!", { RescheduleDeadlinesState(context, userId) }),
      simpleButtonData("Назад", { MenuState(context, userId) }),
    )
  }

  private fun BindingScope<FrontendError>.viewGradesNextState(
    service: StudentApi
  ): QueryAssignmentForCheckingGradesState =
    QueryAssignmentForCheckingGradesState(
      context,
      userId,
      course.id,
      service.getCourseAssignments(course.id).bind(),
    )

  override fun menuState(): State = MenuState(context, userId)
}
