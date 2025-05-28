package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class EditCourseState(override val context: User, private val course: Course) :
  BotStateWithHandlers<State, Unit, Unit> {

  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun outro(bot: BehaviourContext, service: Unit) {
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
    service: Unit,
    updateHandlersController: UpdateHandlersController<BehaviourContext.() -> Unit, State, Any>,
  ) {
    val message =
      bot.send(context, "Изменить курс ${course.name}:", replyMarkup = AdminKeyboards.editCourse())
    sentMessages.add(message)

    updateHandlersController.addDataCallbackHandler { callback ->
      when (callback.data) {
        AdminKeyboards.RETURN_BACK -> NewState(MenuState(context))
        AdminKeyboards.ADD_STUDENT -> NewState(AddStudentState(context, course, course.name))
        AdminKeyboards.REMOVE_STUDENT -> NewState(RemoveStudentState(context, course, course.name))
        AdminKeyboards.ADD_TEACHER -> NewState(AddTeacherState(context, course, course.name))
        AdminKeyboards.REMOVE_TEACHER -> NewState(RemoveTeacherState(context, course, course.name))
        AdminKeyboards.EDIT_DESCRIPTION ->
          NewState(EditDescriptionState(context, course, course.name))
        AdminKeyboards.ADD_SCHEDULED_MESSAGE -> NewState(AddScheduledMessageState(context, course))
        AdminKeyboards.CREATE_ASSIGNMENT -> NewState(CreateAssignmentState(context, course))
        else -> Unhandled
      }
    }
  }

  override fun computeNewState(service: Unit, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: Unit, response: Unit) = Unit
}
