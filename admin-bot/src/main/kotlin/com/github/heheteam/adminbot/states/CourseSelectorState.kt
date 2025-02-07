package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Keyboards
import com.github.heheteam.adminbot.Keyboards.RETURN_BACK
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.toCourseId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.michaelbull.result.get
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

class CourseSelectorState(
  override val context: User,
  private val initNextState: (User, Course?) -> State,
) : BotState<String, Unit, CoursesDistributor> {
  override suspend fun readUserInput(bot: BehaviourContext, service: CoursesDistributor): String {
    val courses = service.getCourses()
    if (courses.isEmpty()) {
      bot.send(context, "Не найдено ни одного курса!")
      return RETURN_BACK
    }

    val message =
      bot.send(context, "Выберите курс:", replyMarkup = Keyboards.buildCoursesSelector(courses))

    val callback = bot.waitDataCallbackQueryWithUser(context.id).first().data
    bot.deleteMessage(message)
    return callback
  }

  override fun computeNewState(service: CoursesDistributor, input: String): Pair<State, Unit> {
    if (input == RETURN_BACK) {
      return Pair(initNextState(context, null), Unit)
    }
    val courseId = input.split(" ").last().toLongOrNull()?.toCourseId() ?: return Pair(this, Unit)
    val course = service.resolveCourse(courseId).get() ?: return Pair(this, Unit)
    val nextState = initNextState(context, course)
    return Pair(nextState, Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    response: Unit,
  ) = Unit
}

// class AssignmentSelectorState(
//  override val context: User,
//  private val courseId: CourseId,
//  private val initNextState: (User, Assignment?) -> State
// ) :
//  BotState<String, Unit, AssignmentStorage> {
//  override suspend fun readUserInput(bot: BehaviourContext, service: AssignmentStorage): String {
//    val assignments = service.getAssignmentsForCourse(courseId)
//    if (assignments.isEmpty()) {
//      bot.send(context, "Не найдено ни одной серии!")
//      return RETURN_BACK
//    }
//
//    val message =
//      bot.send(
//        context,
//        "Выберите серию:",
//        replyMarkup = Keyboards.buildCoursesSelector(assignments),
//      )
//
//    val callback = bot.waitDataCallbackQueryWithUser(context.id).first().data
//    bot.deleteMessage(message)
//    return callback
//  }
//
//  override fun computeNewState(service: AssignmentStorage, input: String): Pair<State, Unit> {
//    if (input == RETURN_BACK) {
//      return Pair(initNextState(context, null), Unit)
//    }
//    val assignmentId = input.split(" ").last().toLongOrNull()?.toAssignmentId()
//      ?: return Pair(this, Unit)
//    val assignment = service.resolveAssignment(assignmentId).get()
//      ?: return Pair(this, Unit)
//    val nextState = initNextState(context, assignment)
//    return Pair(nextState, Unit)
//  }
//
//  override suspend fun sendResponse(bot: BehaviourContext, service: AssignmentStorage, response:
// Unit) {
//  }
// }
