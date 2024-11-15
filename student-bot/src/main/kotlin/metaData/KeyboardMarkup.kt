package com.github.heheteam.studentbot.metaData

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.SolutionType
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

fun buildCoursesSelector(availableCourses: MutableList<Pair<Course, Boolean>>) =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      availableCourses.forEach { (course, status) ->
        val description = if (status) course.description else "${course.description} \u2705"
        row { dataButton(description, "${ButtonKey.COURSE_ID} ${course.id}") }
      }
      row { dataButton("Записаться", ButtonKey.APPLY) }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun buildCoursesSendingSelector(availableCourses: MutableList<Pair<Course, Boolean>>) =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      availableCourses.forEach { (course, status) ->
        if (!status) {
          row { dataButton(course.description, "${ButtonKey.COURSE_ID} ${course.id}") }
        }
      }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun buildSendSolutionSelector() =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      row { dataButton("Текст", SolutionType.TEXT.toString()) }
      row { dataButton("Фото", SolutionType.PHOTOS.toString()) }
      row { dataButton("Файлик", SolutionType.DOCUMENT.toString()) }
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun back() =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      row { dataButton("Назад", ButtonKey.BACK) }
    },
  )

fun menuKeyboard() =
  InlineKeyboardMarkup(
    keyboard =
    matrix {
      row { dataButton("Записаться на курсы", ButtonKey.SIGN_UP) }
      row { dataButton("Посмотреть мои курсы", ButtonKey.VIEW) }
      row { dataButton("Отправить решение", ButtonKey.SEND_SOLUTION) }
    },
  )
