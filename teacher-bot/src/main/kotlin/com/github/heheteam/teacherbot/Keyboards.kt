package com.github.heheteam.teacherbot

import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object Keyboards {
  const val RETURN_BACK = "back"

  fun back() = InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Назад", RETURN_BACK) } })
}
