package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.toCourseId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.flow.first

sealed interface CourseIdError {
  data object BadInteger : CourseIdError

  data object UnresolvedCourse : CourseIdError
}

class ChooseGroupCourseState(override val context: Chat) :
  BotState<CourseId?, Result<Course, CourseIdError>, CoursesDistributor> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: CoursesDistributor,
  ): CourseId? {
    with(bot) {
      sendMessage(context, "Введите id курса")
      val idText = waitTextMessageWithUser(context.id.toChatId()).first().content.text
      return idText.toLongOrNull()?.toCourseId()
    }
  }

  override fun computeNewState(
    service: CoursesDistributor,
    input: CourseId?,
  ): Pair<State, Result<Course, CourseIdError>> {
    val courseOrError =
      input
        .toResultOr { CourseIdError.BadInteger }
        .flatMap { service.resolveCourse(it).mapError { CourseIdError.UnresolvedCourse } }
    return courseOrError.mapBoth(
      success = { ListeningForSolutionsGroupState(context, it.id) },
      failure = { ChooseGroupCourseState(context) },
    ) to courseOrError
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    response: Result<Course, CourseIdError>,
  ) {
    with(bot) {
      response.mapBoth(
        success = { sendMessage(context, "Курс ${it.name}") },
        failure = {
          val text =
            when (it) {
              CourseIdError.BadInteger -> "Unrecognized integer"
              CourseIdError.UnresolvedCourse -> "Unresolved course"
            }
          sendMessage(context, text)
        },
      )
    }
  }
}
