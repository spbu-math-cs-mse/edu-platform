package com.github.heheteam.commonlib.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.Matrix

data class ButtonData<T>(val text: String, val uniqueData: String, val getData: suspend (() -> T))

// handler returns Err<Unit> when wrong button (with unhandled unique data) is pressed
// handler does not delete the menu message, be warned!
data class MenuKeyboardData<T>(
  val keyboard: InlineKeyboardMarkup,
  val handler: suspend (String) -> Result<T, Unit>,
)

fun <T> buildMenu(content: Matrix<ButtonData<T>>): MenuKeyboardData<T> {
  val inlineKeyboard =
    content.map { row ->
      row.map { buttonData ->
        CallbackDataInlineKeyboardButton(buttonData.text, buttonData.uniqueData)
      }
    }
  val callback: suspend (String) -> Result<T, Unit> = { callbackData: String ->
    content
      .flatten()
      .firstOrNull { it.uniqueData == callbackData }
      .toResultOr {} // returns unit
      .map { it.getData.invoke() }
  }
  return MenuKeyboardData(InlineKeyboardMarkup(inlineKeyboard), callback)
}

fun <T> buildColumnMenu(content: List<ButtonData<T>>): MenuKeyboardData<T> =
  buildMenu(content.map { listOf(it) })

fun <T> buildColumnMenu(vararg content: ButtonData<T>): MenuKeyboardData<T> =
  buildMenu(content.map { listOf(it) })
