package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotStateWithHandlers
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

data class QueryCourseForSolutionSendingState(
  override val context: User,
  val studentId: StudentId,
) : BotStateWithHandlers<CourseId?, Unit, CoursesDistributor> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: CoursesDistributor,
    updateHandlersController: UpdateHandlersController<() -> Unit, CourseId?, Any>,
  ) {
    val courses = service.getStudentCourses(studentId)
    val coursesPicker = createCoursePicker(courses)
    val message = bot.sendMessage(context.id, "Выберите курс", replyMarkup = coursesPicker.keyboard)
    sentMessages.add(message)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      coursesPicker
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it?.id) }, failure = { Unhandled })
    }
  }

  override fun computeNewState(service: CoursesDistributor, input: CourseId?): Pair<State, Unit> =
    if (input != null) QueryProblemForSolutionSendingState(context, studentId, input) to Unit
    else {
      MenuState(context, studentId) to Unit
    }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    response: Unit,
  ) {
    for (message in sentMessages) {
      bot.delete(message)
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: CoursesDistributor) = Unit
}
