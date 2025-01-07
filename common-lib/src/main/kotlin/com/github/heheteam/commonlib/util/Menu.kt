package com.github.heheteam.commonlib.util

import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.Matrix

data class ButtonData<T>(val text: String, val uniqueData: String, val getData: (() -> T))

// handler returns null when wrong button (with unhandled unique data) is pressed
// handler does not delete the menu message, be warned!
data class MenuKeyboardData<T : Any>(
  val keyboard: InlineKeyboardMarkup,
  val handler: (String) -> T?,
)

fun <T : Any> buildMenu(content: Matrix<ButtonData<T>>): MenuKeyboardData<T> {
  val inlineKeyboard =
    content.map { row ->
      row.map { buttonData ->
        CallbackDataInlineKeyboardButton(buttonData.text, buttonData.uniqueData)
      }
    }
  val callback = asdf@{ callbackData: String ->
    content.flatten().firstOrNull { it.uniqueData == callbackData }?.getData?.invoke()
  }
  return MenuKeyboardData(InlineKeyboardMarkup(inlineKeyboard), callback)
}

fun <T : Any> buildColumnMenu(content: List<ButtonData<T>>): MenuKeyboardData<T> =
  buildMenu(content.map { listOf(it) })
