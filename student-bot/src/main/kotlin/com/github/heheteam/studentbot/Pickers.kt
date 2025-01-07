package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.state.BotState
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.coroutines.flow.first

internal suspend fun BehaviourContext.queryCourse(
  state: BotState,
  courses: List<Course>,
  messageContent: String = Dialogues.askCourseForSolution(),
): Course? {
  val courseSelector =
    buildColumnMenu(
      courses.map { course ->
        ButtonData(course.name, "${ButtonKey.COURSE_ID} ${course.id}") {
          Ok(course) as Result<Course, Unit>
        }
      } + ButtonData("Назад", ButtonKey.BACK) { Err(Unit) }
    )
  val message = bot.send(state.context, messageContent, replyMarkup = courseSelector.keyboard)

  val callbackData = waitDataCallbackQueryWithUser(state.context.id).first().data
  deleteMessage(message)
  val result = courseSelector.handler(callbackData)?.mapBoth(success = { it }, failure = { null })
  return result
}
