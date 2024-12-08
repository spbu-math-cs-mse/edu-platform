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
    InlineKeyboardMarkup(
      keyboard =
      matrix {
        row {
          dataButton("Назад \uD83D\uDD19", returnBack)
        }
      },
    )

  val createCourse = "create course"
  val getTeachers = "get teachers"
  val getProblems = "get problems"

  fun menu() =
    inlineKeyboard {
      row {
        dataButton("➕ Создать курс", createCourse)
      }
      row {
        dataButton("Изменить курс", "edit course")
      }
      row {
        dataButton("Список всех преподавателей", getTeachers)
      }
      row {
        dataButton("Список всех серий с задачами", getProblems)
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

  val courseId = "courseId"
  fun buildCoursesSelector(availableCourses: List<Course>) =
    InlineKeyboardMarkup(
      keyboard =
      matrix {
        availableCourses.forEach { course ->
          row { dataButton(course.name, "$courseId ${course.id.id}") }
        }
        row { dataButton("Назад \uD83D\uDD19", returnBack) }
      },
    )
}
