package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.Keyboards.CHALLENGE
import com.github.heheteam.studentbot.Keyboards.CHECK_DEADLINES
import com.github.heheteam.studentbot.Keyboards.CHECK_GRADES
import com.github.heheteam.studentbot.Keyboards.RESCHEDULE_DEADLINES
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.Keyboards.SEND_SOLUTION
import com.github.heheteam.studentbot.state.CheckDeadlinesState
import com.github.heheteam.studentbot.state.MenuState
import com.github.heheteam.studentbot.state.QueryAssignmentForCheckingGradesState
import com.github.heheteam.studentbot.state.QueryProblemForSubmissionSendingState
import com.github.heheteam.studentbot.state.RequestChallengeState
import com.github.heheteam.studentbot.state.RescheduleDeadlinesState
import com.github.heheteam.studentbot.state.StudentKeyboards
import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.binding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities

class CourseMenuState(
  override val context: User,
  override val userId: StudentId,
  val course: Course,
) : SimpleState<StudentApi, StudentId>() {
  fun handleKeyboardCallback(data: String, service: StudentApi) = binding {
    when (data) {
      SEND_SOLUTION -> QueryProblemForSubmissionSendingState(context, userId, course.id)
      CHECK_GRADES -> viewGradesNextState(service)
      CHECK_DEADLINES -> CheckDeadlinesState(context, userId, course)
      RESCHEDULE_DEADLINES -> RescheduleDeadlinesState(context, userId)
      CHALLENGE -> RequestChallengeState(context, userId, course)
      RETURN_BACK -> MenuState(context, userId)
      else -> null
    }
  }

  override suspend fun BotContext.run(service: StudentApi) {
    service.saveSelectedCourse(userId, course.id)
    send(buildEntities { +"Меню курса \"${course.name}\"" }, StudentKeyboards.courseMenu())
      .deleteLater()
    addDataCallbackHandler { callback ->
      handleKeyboardCallback(callback.data, service).value?.let { NewState(it) } ?: Unhandled
    }
  }

  override fun defaultState(): State = MenuState(context, userId)

  private fun BindingScope<FrontendError>.viewGradesNextState(
    service: StudentApi
  ): QueryAssignmentForCheckingGradesState =
    QueryAssignmentForCheckingGradesState(
      context,
      userId,
      course.id,
      service.getCourseAssignments(course.id).bind(),
    )
}
