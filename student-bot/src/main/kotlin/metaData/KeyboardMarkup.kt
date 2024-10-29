package com.github.heheteam.samplebot.metaData

import com.github.heheteam.samplebot.data.Course
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

fun buildCoursesSelector(availableCourses: Collection<Course>) = InlineKeyboardMarkup(
  keyboard = matrix {
    availableCourses.forEach { course ->
      row { dataButton(course.description, "${ButtonKey.COURSE_ID} ${course.id}") }
    }
    row { dataButton("Записаться", ButtonKey.APPLY) }
    row { dataButton("Назад", ButtonKey.BACK) }
  },
)

fun back() = InlineKeyboardMarkup(
  keyboard = matrix {
    row { dataButton("Назад", ButtonKey.BACK) }
  },
)

fun menuKeyboard() = InlineKeyboardMarkup(
  keyboard = matrix {
    row { dataButton("Записаться на курсы", ButtonKey.SIGN_UP) }
    row { dataButton("Посмотреть мои курсы", ButtonKey.VIEW) }
  },
)
