package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.AdminKeyboards.COURSE_INFO
import com.github.heheteam.adminbot.AdminKeyboards.CREATE_ASSIGNMENT
import com.github.heheteam.adminbot.AdminKeyboards.CREATE_COURSE
import com.github.heheteam.adminbot.AdminKeyboards.EDIT_COURSE
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.queryCourse
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent

class MenuState(override val context: User, val adminId: AdminId) :
  BotStateWithHandlers<State, Unit, AdminApi> {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<State>,
  ) {
    val menuMessage = bot.send(context, Dialogues.menu, replyMarkup = AdminKeyboards.menu())
    sentMessages.add(menuMessage)

    updateHandlersController.addDataCallbackHandler { callback ->
      when (callback.data) {
        CREATE_COURSE -> NewState(CreateCourseState(context, adminId))
        EDIT_COURSE -> NewState(QueryCourseForEditing(context, adminId))
        CREATE_ASSIGNMENT -> NewState(QueryCourseForAssignmentCreation(context, adminId))
        COURSE_INFO -> {
          val courses = service.getCourses().map { it.value }
          bot.queryCourse(context, courses)?.let { course ->
            NewState(CourseInfoState(context, course, adminId))
          } ?: Unhandled
        }

        else -> Unhandled
      }
    }
  }

  override fun computeNewState(service: AdminApi, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: State,
  ) = Unit
}
