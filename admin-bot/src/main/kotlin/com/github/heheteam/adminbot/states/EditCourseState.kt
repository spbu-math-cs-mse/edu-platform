package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class EditCourseState(override val context: User, private val course: Course) :
  BotState<String, Unit, Unit> {
  override suspend fun readUserInput(bot: BehaviourContext, service: Unit): String {
    bot.send(context, "Изменить курс ${course.name}:", replyMarkup = Keyboards.editCourse())
    return bot.waitDataCallbackQueryWithUser(context.id).first().data
  }

  override fun computeNewState(service: Unit, input: String): Pair<State, Unit> {
    val courseName = course.name
    val nextState =
      when (input) {
        Keyboards.RETURN_BACK -> MenuState(context)

        Keyboards.ADD_STUDENT -> AddStudentState(context, course, courseName)

        Keyboards.REMOVE_STUDENT -> RemoveStudentState(context, course, courseName)

        Keyboards.ADD_TEACHER -> AddTeacherState(context, course, courseName)

        Keyboards.REMOVE_TEACHER -> RemoveTeacherState(context, course, courseName)

        Keyboards.EDIT_DESCRIPTION -> EditDescriptionState(context, course, courseName)

        Keyboards.ADD_SCHEDULED_MESSAGE -> AddScheduledMessageState(context, course, courseName)

        else -> EditCourseState(context, course)
      }
    return Pair(nextState, Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: Unit, response: Unit) = Unit
}
