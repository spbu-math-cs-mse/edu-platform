package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.state.BotStateWithHandlers
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

data class QueryCourseForAssignmentCreation(override val context: User) :
  BotStateWithHandlers<Course?, Unit, AdminCore> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminCore,
    updateHandlersController: UpdateHandlersController<() -> Unit, Course?, Any>,
  ) {
    val courses = service.getCourses().map { it.value }
    val coursesPicker = createCoursePicker(courses)
    val message = bot.sendMessage(context.id, "Выберите курс", replyMarkup = coursesPicker.keyboard)
    sentMessages.add(message)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      coursesPicker
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override fun computeNewState(service: AdminCore, input: Course?): Pair<State, Unit> =
    if (input != null) CreateAssignmentState(context, input) to Unit
    else {
      MenuState(context) to Unit
    }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminCore, response: Unit) {
    for (message in sentMessages) {
      bot.delete(message)
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: AdminCore) = Unit
}
