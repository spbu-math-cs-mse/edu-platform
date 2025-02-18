package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.CourseStatistics
import com.github.heheteam.adminbot.CourseStatisticsComposer
import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.formatters.CourseStatisticsFormatter
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class CourseInfoState(override val context: User, val course: Course) :
  BotState<Unit, CourseStatistics, CourseStatisticsComposer> {
  override suspend fun readUserInput(bot: BehaviourContext, service: CourseStatisticsComposer) =
    Unit

  override fun computeNewState(
    service: CourseStatisticsComposer,
    input: Unit,
  ): Pair<State, CourseStatistics> {
    val stats = service.getCourseStatistics(course.id)
    return Pair(MenuState(context), stats)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CourseStatisticsComposer,
    response: CourseStatistics,
  ) {
    val statsMessage =
      bot.send(
        context,
        entities = CourseStatisticsFormatter.format(course.name, response),
        replyMarkup = Keyboards.returnBack(),
      )
    bot.waitDataCallbackQueryWithUser(context.id).first()
    bot.deleteMessage(statsMessage)
  }
}
