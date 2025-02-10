package com.github.heheteam.adminbot

import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object Keyboards {
  const val RETURN_BACK = "Назад"

  fun returnBack() =
    InlineKeyboardMarkup(
      keyboard = matrix { row { dataButton("Назад \uD83D\uDD19", RETURN_BACK) } }
    )

  const val CREATE_COURSE = "create course"
  const val EDIT_COURSE = "edit course"
  const val GET_TEACHERS = "get teachers"
  const val GET_PROBLEMS = "get problems"
  const val CREATE_ASSIGNMENT = "create assignment"
  const val COURSE_INFO = "course info"

  fun menu() = inlineKeyboard {
    row { dataButton("➕ Создать курс", CREATE_COURSE) }
    row { dataButton("Изменить курс", EDIT_COURSE) }
    row { dataButton("Информация о курсе", COURSE_INFO) }
    row { dataButton("Список всех преподавателей", GET_TEACHERS) }
    row { dataButton("Список всех серий с задачами", GET_PROBLEMS) }
    row { dataButton("Создать серию", CREATE_ASSIGNMENT) }
  }

  const val ADD_STUDENT = "add a student"
  const val REMOVE_STUDENT = "remove a student"
  const val ADD_TEACHER = "add a teacher"
  const val REMOVE_TEACHER = "remove a teacher"
  const val EDIT_DESCRIPTION = "edit description"
  const val ADD_SCHEDULED_MESSAGE = "add scheduled message"

  fun editCourse() = inlineKeyboard {
    row { dataButton("Добавить учеников", ADD_STUDENT) }
    row { dataButton("Убрать учеников", REMOVE_STUDENT) }
    row { dataButton("Добавить преподавателей", ADD_TEACHER) }
    row { dataButton("Убрать преподавателей", REMOVE_TEACHER) }
    row { dataButton("Изменить описание", EDIT_DESCRIPTION) }
    row { dataButton("Добавить отложенное сообщение", ADD_SCHEDULED_MESSAGE) }
    row { dataButton("Назад", RETURN_BACK) }
  }
}
