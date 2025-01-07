package com.github.heheteam.adminbot

import com.github.heheteam.commonlib.Course
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object Keyboards {
  val returnBack = "Назад"

  fun returnBack() =
    InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Назад \uD83D\uDD19", returnBack) } })

  val createCourse = "create course"
  val getTeachers = "get teachers"
  val getProblems = "get problems"
  val createAssignment = "create assignment"
  val courseInfo = "course info"

  fun menu() = inlineKeyboard {
    row { dataButton("➕ Создать курс", createCourse) }
    row { dataButton("Изменить курс", "edit course") }
    row { dataButton("Информация о курсе", courseInfo) }
    row { dataButton("Список всех преподавателей", getTeachers) }
    row { dataButton("Список всех серий с задачами", getProblems) }
    row { dataButton("Создать серию", createAssignment) }
  }

  val addStudent = "add a student"
  val removeStudent = "remove a student"
  val addTeacher = "add a teacher"
  val removeTeacher = "remove a teacher"
  val editDescription = "edit description"
  val addScheduledMessage = "add scheduled message"

  fun editCourse() = inlineKeyboard {
    row { dataButton("Добавить учеников", addStudent) }
    row { dataButton("Убрать учеников", removeStudent) }
    row { dataButton("Добавить преподавателей", addTeacher) }
    row { dataButton("Убрать преподавателей", removeTeacher) }
    row { dataButton("Изменить описание", editDescription) }
    row { dataButton("Добавить отложенное сообщение", addScheduledMessage) }
    row { dataButton("Назад", returnBack) }
  }

  val courseId = "courseId"

  fun buildCoursesSelector(availableCourses: List<Course>) =
    InlineKeyboardMarkup(
      keyboard =
        matrix {
          availableCourses.forEach { course ->
            row { dataButton(course.name, "$courseId ${course.id}") }
          }
          row { dataButton("Назад \uD83D\uDD19", returnBack) }
        }
    )
}
