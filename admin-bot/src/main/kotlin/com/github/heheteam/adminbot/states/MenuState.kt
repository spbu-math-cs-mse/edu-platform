package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.Keyboards.COURSE_INFO
import com.github.heheteam.adminbot.Keyboards.CREATE_ASSIGNMENT
import com.github.heheteam.adminbot.Keyboards.CREATE_COURSE
import com.github.heheteam.adminbot.Keyboards.EDIT_COURSE
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.queryCourse
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

class MenuState(override val context: User) : BotState<State, Unit, CoursesDistributor> {
  override suspend fun readUserInput(bot: BehaviourContext, service: CoursesDistributor): State {
    val initialMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())
    val newState =
      bot
        .waitDataCallbackQueryWithUser(context.id)
        .mapNotNull { callback ->
          when (callback.data) {
            CREATE_COURSE -> CreateCourseState(context)

            EDIT_COURSE -> {
              QueryCourseForEditing(context)
            }

            CREATE_ASSIGNMENT -> {
              QueryCourseForAssignmentCreation(context)
            }

            COURSE_INFO -> {
              val courses = service.getCourses()
              bot.queryCourse(context, courses)?.let { course -> CourseInfoState(context, course) }
            }

            else -> null
          }
        }
        .first()

    bot.deleteMessage(initialMessage)
    return newState
  }

  override fun computeNewState(service: CoursesDistributor, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    response: Unit,
  ) = Unit
}
