package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class EditCourseState(override val context: User, private val course: Course) :
  BotStateWithHandlers<State, Unit, Unit> {

  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun outro(bot: BehaviourContext, service: Unit) {
    sentMessages.forEach { bot.delete(it) }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: Unit,
    updateHandlersController: UpdateHandlersController<() -> Unit, State, Any>,
  ) {
    val message =
      bot.send(context, "Изменить курс ${course.name}:", replyMarkup = Keyboards.editCourse())
    sentMessages.add(message)

    updateHandlersController.addTextMessageHandler { commandMessage ->
      if (commandMessage.content.text == "/menu") {
        NewState(MenuState(context))
      } else {
        Unhandled
      }
    }

    updateHandlersController.addDataCallbackHandler { callback ->
      when (callback.data) {
        Keyboards.RETURN_BACK -> NewState(MenuState(context))
        Keyboards.ADD_STUDENT -> NewState(AddStudentState(context, course, course.name))
        Keyboards.REMOVE_STUDENT -> NewState(RemoveStudentState(context, course, course.name))
        Keyboards.ADD_TEACHER -> NewState(AddTeacherState(context, course, course.name))
        Keyboards.REMOVE_TEACHER -> NewState(RemoveTeacherState(context, course, course.name))
        Keyboards.EDIT_DESCRIPTION -> NewState(EditDescriptionState(context, course, course.name))
        Keyboards.ADD_SCHEDULED_MESSAGE -> NewState(AddScheduledMessageState(context, course))
        Keyboards.CREATE_ASSIGNMENT -> NewState(CreateAssignmentState(context, course))
        else -> Unhandled
      }
    }
  }

  override fun computeNewState(service: Unit, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: Unit, response: Unit) = Unit
}
