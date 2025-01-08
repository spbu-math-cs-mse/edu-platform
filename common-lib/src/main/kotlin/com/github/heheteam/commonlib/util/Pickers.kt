package com.github.heheteam.commonlib.util

import com.github.michaelbull.result.mapBoth
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.first

suspend fun <T> BehaviourContext.queryPickerWithBackFromList(
  user: User,
  objects: List<T>,
  objToButtonText: (T) -> String,
  queryText: String,
): T? {
  val buttonData = "button"
  val backData = "back"
  val objToUniqueText: (T) -> String = objToButtonText // it bust be unique, right?
  val objectSelector =
    buildColumnMenu(
      objects.map { obj ->
        ButtonData(objToButtonText(obj), "$buttonData ${objToUniqueText(obj)}") { obj as T? }
      } + ButtonData("Назад", backData) { null }
    )

  val message = bot.send(user, queryText, replyMarkup = objectSelector.keyboard)
  val callbackData = waitDataCallbackQueryWithUser(user.id).first().data
  deleteMessage(message)
  val result = objectSelector.handler(callbackData).mapBoth(success = { it }, failure = { null })
  return result
}
