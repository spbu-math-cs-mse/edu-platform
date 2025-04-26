package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.createCoursePicker
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

abstract class QueryCourseState(override val context: User) :
  BotStateWithHandlers<Course?, Unit, AdminApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersController<BehaviourContext.() -> Unit, Course?, Any>,
  ) {
    val courses = service.getCourses().map { it.value }
    val coursesPicker = createCoursePicker(courses)
    val message = bot.sendMessage(context.id, "Выберите курс", replyMarkup = coursesPicker.keyboard)
    sentMessages.add(message)
    updateHandlersController.addTextMessageHandler { maybeCommandMessage ->
      if (maybeCommandMessage.content.text == "/menu") {
        NewState(MenuState(context))
      } else {
        Unhandled
      }
    }
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      coursesPicker
        .handler(dataCallbackQuery.data)
        .mapBoth(success = { UserInput(it) }, failure = { Unhandled })
    }
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: Unit) = Unit

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach { bot.delete(it) }
  }
}
