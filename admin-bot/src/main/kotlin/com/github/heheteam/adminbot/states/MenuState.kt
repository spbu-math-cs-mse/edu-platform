package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.Keyboards.COURSE_INFO
import com.github.heheteam.adminbot.Keyboards.CREATE_ASSIGNMENT
import com.github.heheteam.adminbot.Keyboards.CREATE_COURSE
import com.github.heheteam.adminbot.Keyboards.EDIT_COURSE
import com.github.heheteam.adminbot.Keyboards.GET_PROBLEMS
import com.github.heheteam.adminbot.Keyboards.GET_TEACHERS
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class MenuState(override val context: User) : BotState<State, Unit, Unit> {
  override suspend fun readUserInput(bot: BehaviourContext, service: Unit): State {
    val initialMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())

    val callback = bot.waitDataCallbackQueryWithUser(context.id).first()
    val data = callback.data
    //    bot.answerCallbackQuery(callback)
    bot.deleteMessage(initialMessage)
    return when (data) {
      CREATE_COURSE -> CreateCourseState(context)

      EDIT_COURSE ->
        CourseSelectorState(context) { context, course ->
          if (course == null) MenuState(context) else EditCourseState(context, course)
        }

      GET_TEACHERS -> GetTeachersState(context)

      GET_PROBLEMS ->
        CourseSelectorState(context) { context, course ->
          if (course == null) MenuState(context) else GetProblemsState(context, course)
        }

      CREATE_ASSIGNMENT ->
        CourseSelectorState(context) { context, course ->
          if (course == null) MenuState(context) else CreateAssignmentState(context, course)
        }

      COURSE_INFO ->
        CourseSelectorState(context) { context, course ->
          if (course == null) MenuState(context) else CourseInfoState(context, course)
        }

      else -> MenuState(context)
    }
  }

  override fun computeNewState(service: Unit, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: Unit, response: Unit) = Unit
}
