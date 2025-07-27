package com.github.heheteam.adminbot

import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object AdminKeyboards {
  const val RETURN_BACK = "Назад"

  fun returnBack() =
    InlineKeyboardMarkup(
      keyboard = matrix { row { dataButton("Назад \uD83D\uDD19", RETURN_BACK) } }
    )

  fun tryAgain() =
    InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Попробовать снова", RETURN_BACK) } })

  const val CREATE_COURSE = "create course"
  const val EDIT_COURSE = "edit course"
  const val CREATE_ASSIGNMENT = "create assignment"
  const val COURSE_INFO = "course info"
  const val ADD_ADMIN = "add admin"
  const val SEND_SCHEDULED = "send scheduled"

  fun menu() = inlineKeyboard {
    row { dataButton("➕ Создать курс", CREATE_COURSE) }
    row { dataButton("➖ Изменить курс", EDIT_COURSE) }
    row { dataButton("❔ Информация о курсе", COURSE_INFO) }
    row { dataButton("\uD83D\uDEC2 Добавить администратора", ADD_ADMIN) }
    row { dataButton("⌚ Отложенные сообщение", SEND_SCHEDULED) }
  }

  const val ADD_STUDENT = "add a student"
  const val REMOVE_STUDENT = "remove a student"
  const val ADD_TEACHER = "add a teacher"
  const val REMOVE_TEACHER = "remove a teacher"
  const val EDIT_DESCRIPTION = "edit description"
  const val ADD_SCHEDULED_MESSAGE = "add scheduled message"
  const val VIEW_SCHEDULED_MESSAGES = "view scheduled messages"
  const val DELETE_SCHEDULED_MESSAGE = "delete scheduled message"

  const val REGENERATE_TOKEN = "regenerate token"

  fun courseInfo(ratingUrl: String?, token: String?) = inlineKeyboard {
    if (ratingUrl != null) row { urlButton("Кондуит", ratingUrl) }
    if (token != null) row { dataButton("Обновить токен", REGENERATE_TOKEN) }
    else row { dataButton("Создать токен", REGENERATE_TOKEN) }
    row {
      dataButton("Посмотреть запланированные сообщения", AdminKeyboards.VIEW_SCHEDULED_MESSAGES)
    }
    row { dataButton("Назад", RETURN_BACK) }
  }

  const val MOVE_DEADLINES = "moveDeadlines"
}
