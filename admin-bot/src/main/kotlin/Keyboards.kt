package com.github.heheteam.adminbot

import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object Keyboards {
  val returnBack = "Назад"

  fun returnBack() =
    InlineKeyboardMarkup(
      keyboard =
      matrix {
        row {
          dataButton("Назад \uD83D\uDD19", returnBack)
        }
      },
    )

  val createCourse = "create course"

  fun menu() =
    inlineKeyboard {
      row {
        dataButton("➕ Создать курс", createCourse)
      }
      row {
        dataButton("Изменить курс", "edit course")
      }
    }

  val addStudent = "add a student"
  val removeStudent = "remove a student"
  val addTeacher = "add a teacher"
  val removeTeacher = "remove a teacher"
  val editDescription = "edit description"
  val addScheduledMessage = "add scheduled message"

  fun editCourse() =
    inlineKeyboard {
      row {
        dataButton("Добавить ученика", addStudent)
      }
      row {
        dataButton("Убрать ученика", removeStudent)
      }
      row {
        dataButton("Добавить преподавателя", addTeacher)
      }
      row {
        dataButton("Убрать преподавателя", removeTeacher)
      }
      row {
        dataButton("Изменить описание", editDescription)
      }
      row {
        dataButton("Добавить отложенное сообщение", addScheduledMessage)
      }
      row {
        dataButton("Назад", returnBack)
      }
    }
}
