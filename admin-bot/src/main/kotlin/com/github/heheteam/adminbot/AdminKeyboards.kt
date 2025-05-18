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

  const val CREATE_COURSE = "create course"
  const val EDIT_COURSE = "edit course"
  const val CREATE_ASSIGNMENT = "create assignment"
  const val COURSE_INFO = "course info"

  fun menu() = inlineKeyboard {
    row { dataButton("➕ Создать курс", CREATE_COURSE) }
    row { dataButton("➖ Изменить курс", EDIT_COURSE) }
    row { dataButton("❔ Информация о курсе", COURSE_INFO) }
  }

  const val ADD_STUDENT = "add a student"
  const val REMOVE_STUDENT = "remove a student"
  const val ADD_TEACHER = "add a teacher"
  const val REMOVE_TEACHER = "remove a teacher"
  const val EDIT_DESCRIPTION = "edit description"
  const val ADD_SCHEDULED_MESSAGE = "add scheduled message"

  fun editCourse() = inlineKeyboard {
    row { dataButton("➕ Создать серию", CREATE_ASSIGNMENT) }
    row { dataButton("➕ Добавить учеников", ADD_STUDENT) }
    row { dataButton("➖ Убрать учеников", REMOVE_STUDENT) }
    row { dataButton("➕ Добавить преподавателей", ADD_TEACHER) }
    row { dataButton("➖ Убрать преподавателей", REMOVE_TEACHER) }
    row { dataButton("\uD83D\uDD04 Изменить описание", EDIT_DESCRIPTION) }
    row { dataButton("➕ Добавить отложенное сообщение", ADD_SCHEDULED_MESSAGE) }
    row { dataButton("Назад \uD83D\uDD19", RETURN_BACK) }
  }

  const val REGENERATE_TOKEN = "regenerate token"

  fun courseInfo(ratingUrl: String?, token: String?) = inlineKeyboard {
    if (ratingUrl != null) row { urlButton("Кондуит", ratingUrl) }
    if (token != null) row { dataButton("Обновить токен", REGENERATE_TOKEN) }
    else row { dataButton("Создать токен", REGENERATE_TOKEN) }
    row { dataButton("Назад", RETURN_BACK) }
  }

  const val MOVE_DEADLINES = "moveDeadlines"
}
