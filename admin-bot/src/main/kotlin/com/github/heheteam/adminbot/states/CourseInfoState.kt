package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.formatters.CourseStatisticsFormatter
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.CourseStatistics
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class CourseInfoState(override val context: User, val course: Course) :
  BotState<Unit, CourseStatistics, AdminApi> {
  override suspend fun readUserInput(bot: BehaviourContext, service: AdminApi) = Unit

  override fun computeNewState(service: AdminApi, input: Unit): Pair<State, CourseStatistics> {
    val stats = service.getCourseStatistics(course.id)
    return Pair(MenuState(context), stats)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: CourseStatistics,
  ) {
    bot.send(
      context,
      entities = CourseStatisticsFormatter.format(course.name, response),
      replyMarkup = Keyboards.courseInfo(service.getRatingLink(course.id).value),
    )
  }
}
