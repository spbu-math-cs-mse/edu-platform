package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.formatters.CourseStatisticsFormatter
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class CourseInfoState(override val context: User, val course: Course, val adminId: AdminId) :
  BotStateWithHandlers<Unit, Unit, AdminApi> {
  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<Unit>,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val stats = service.getCourseStatistics(course.id)
    val courseToken = service.getTokenForCourse(course.id)
    bot.send(
      context,
      entities = CourseStatisticsFormatter.format(course.id, course.name, stats, courseToken),
      replyMarkup = AdminKeyboards.courseInfo(service.getRatingLink(course.id).value, courseToken),
    )

    updateHandlersController.addDataCallbackHandler { callback ->
      when (callback.data) {
        AdminKeyboards.RETURN_BACK -> NewState(MenuState(context, adminId))
        AdminKeyboards.REGENERATE_TOKEN -> {
          service.regenerateTokenForCourse(course.id)
          NewState(CourseInfoState(context, course, adminId))
        }
        AdminKeyboards.VIEW_SCHEDULED_MESSAGES -> {
          NewState(QueryNumberOfRecentMessagesState(context, adminId, course.id))
        }

        else -> Unhandled
      }
    }
  }

  override suspend fun computeNewState(service: AdminApi, input: Unit): Pair<State, Unit> =
    Pair(MenuState(context, adminId), Unit)

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: Unit,
  ) = Unit

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) = Unit
}
