package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

class EditCourseState(override val context: User, private val course: Course) :
  BotState<State, Unit, Unit> {
  override suspend fun readUserInput(bot: BehaviourContext, service: Unit): State {
    val message =
      bot.send(context, "Изменить курс ${course.name}:", replyMarkup = Keyboards.editCourse())
    val newState =
      bot
        .waitDataCallbackQueryWithUser(context.id)
        .mapNotNull { callback ->
          when (callback.data) {
            Keyboards.RETURN_BACK -> MenuState(context)

            Keyboards.ADD_STUDENT -> AddStudentState(context, course, course.name)

            Keyboards.REMOVE_STUDENT -> RemoveStudentState(context, course, course.name)

            Keyboards.ADD_TEACHER -> AddTeacherState(context, course, course.name)

            Keyboards.REMOVE_TEACHER -> RemoveTeacherState(context, course, course.name)

            Keyboards.EDIT_DESCRIPTION -> EditDescriptionState(context, course, course.name)

            Keyboards.ADD_SCHEDULED_MESSAGE -> AddScheduledMessageState(context, course)

            Keyboards.CREATE_ASSIGNMENT -> CreateAssignmentState(context, course)

            else -> null
          }
        }
        .first()

    bot.delete(message)
    return newState
  }

  override fun computeNewState(service: Unit, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: Unit, response: Unit) = Unit
}
