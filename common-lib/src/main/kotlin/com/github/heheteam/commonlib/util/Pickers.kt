package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
      .map { objectSelector.handler(it.data) }
      .filter { it.isOk }
      .first()
      .value
  try {
    deleteMessage(message)
  } catch (e: CommonRequestException) {
    KSLog.warning("Failed to delete message", e)
  }
  return result
}

fun <T> createPickerWithBackButtonFromList(
  objects: List<T>,
  objToButtonText: (T) -> String,
): MenuKeyboardData<T?> {
  val buttonData = "button"
  val backData = "back"
  val objectSelector =
    buildColumnMenu(
      objects.mapIndexed { index, obj ->
        ButtonData(objToButtonText(obj), "$buttonData $index") { obj as T? }
      } + ButtonData("Назад", backData) { null }
    )
  return objectSelector
}

suspend fun BehaviourContext.queryCourse(user: User, courses: List<Course>): Course? =
  queryPickerWithBackFromList(user, courses, "Выберите курс") { it.name }

fun createCoursePicker(courses: List<Course>): MenuKeyboardData<Course?> =
  createPickerWithBackButtonFromList(courses) { it.name }

fun createAssignmentPicker(assignments: List<Assignment>): MenuKeyboardData<Assignment?> =
  createPickerWithBackButtonFromList(assignments) { it.description }

suspend fun BehaviourContext.queryAssignment(
  user: User,
  assignments: List<Assignment>,
): Assignment? = queryPickerWithBackFromList(user, assignments, "Выберите серию") { it.description }
