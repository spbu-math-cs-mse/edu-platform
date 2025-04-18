package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.Keyboards.COURSE_INFO
import com.github.heheteam.adminbot.Keyboards.CREATE_ASSIGNMENT
import com.github.heheteam.adminbot.Keyboards.CREATE_COURSE
import com.github.heheteam.adminbot.Keyboards.EDIT_COURSE
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.queryCourse
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

class MenuState(override val context: User) : BotStateWithHandlers<State, Unit, AdminApi> {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach { bot.delete(it) }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, State, Any>,
  ) {
    val menuMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())
    sentMessages.add(menuMessage)

    updateHandlersController.addTextMessageHandler { message ->
      if (message.content.text == "/menu") {
        NewState(MenuState(context))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addDataCallbackHandler { callback ->
      when (callback.data) {
        CREATE_COURSE -> NewState(CreateCourseState(context))
        EDIT_COURSE -> NewState(QueryCourseForEditing(context))
        CREATE_ASSIGNMENT -> NewState(QueryCourseForAssignmentCreation(context))
        COURSE_INFO -> {
          val courses = service.getCourses().map { it.value }
          bot.queryCourse(context, courses)?.let { course ->
            NewState(CourseInfoState(context, course))
          } ?: Unhandled
        }

        else -> Unhandled
      }
    }
  }

  override fun computeNewState(service: AdminApi, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: AdminApi, response: Unit) = Unit
}
