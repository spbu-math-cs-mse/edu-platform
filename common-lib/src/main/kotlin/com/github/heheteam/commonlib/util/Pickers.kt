package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

suspend fun <T> BehaviourContext.queryPickerWithBackFromList(
  user: User,
  objects: List<T>,
  queryText: String,
  objToButtonText: (T) -> String,
): T? {
  val buttonData = "button"
  val backData = "back"
  val objectSelector =
    buildColumnMenu(
      objects.mapIndexed { index, obj ->
        ButtonData(objToButtonText(obj), "$buttonData $index") { obj as T? }
      } + ButtonData("Назад", backData) { null }
    )

  val message = bot.send(user, queryText, replyMarkup = objectSelector.keyboard)
  val result =
    waitDataCallbackQueryWithUser(user.id)
      .mapNotNull { objectSelector.handler(it.data).get() }
      .first()
  deleteMessage(message)
  return result
}

suspend fun BehaviourContext.queryCourse(user: User, courses: List<Course>): Course? =
  queryPickerWithBackFromList(user, courses, "Выберите курс") { it.name }

suspend fun BehaviourContext.queryAssignment(
  user: User,
  assignments: List<Assignment>,
): Assignment? = queryPickerWithBackFromList(user, assignments, "Выберите серию") { it.description }
