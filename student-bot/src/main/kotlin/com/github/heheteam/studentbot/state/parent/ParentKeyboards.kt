package com.github.heheteam.studentbot.state.parent

import com.github.heheteam.commonlib.Course
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object ParentKeyboards {
  const val RETURN_BACK = "returnBack"
  const val ABOUT_COURSE = "aboutCourse"
  const val FREE_ACTIVITY = "freeActivity"
  const val MAXIMOV = "maximov"
  const val KAMEN = "kamen"
  const val SOLUTIONS = "solutions"

  fun menu() = inlineKeyboard {
    row { dataButton("\uD83D\uDCD6 Подробнее о курсе", ABOUT_COURSE) }
    row { urlButton("\uD83D\uDECD Купить курс", "https://dabromat.ru/start") }
    row {
      urlButton("\uD83D\uDCE2 Подписаться на наш Telegram-канал", "https://t.me/+4brJbkfpd7xhNWE6")
    }
    row { urlButton("❓ Задать вопрос", "https://t.me/m/7qK9yUD5YWE6") }
    row { dataButton("\uD83C\uDFAE Квест от Таксы Дуси", FREE_ACTIVITY) }
    row { dataButton("Разборы задач", SOLUTIONS) }
  }

  const val ABOUT_TEACHERS = "aboutTeachers"
  const val METHODOLOGY = "methodology"
  const val COURSE_RESULTS = "courseResults"

  fun aboutCourse() = inlineKeyboard {
    row { urlButton("\uD83D\uDECD Купить курс", "https://dabromat.ru/start") }
    row { dataButton("О преподавателях", ABOUT_TEACHERS) }
    row { dataButton("Методика, как все будет устроено?", METHODOLOGY) }
    row { dataButton("Результаты курса", COURSE_RESULTS) }
    row {
      urlButton(
        "Другие наши курсы",
        "https://dabromat.ru/?utm_source=quest&utm_medium=button&utm_content=link",
      )
    }
    row {
      urlButton("\uD83D\uDCE2 Подписаться на наш Telegram-канал", "https://t.me/+4brJbkfpd7xhNWE6")
    }
    row { urlButton("❓ Задать вопрос", "https://t.me/m/7qK9yUD5YWE6") }
    row { dataButton("\uD83D\uDD19 Назад", RETURN_BACK) }
  }

  fun defaultKeyboard(includeMax: Boolean = false, includeKamen: Boolean = false) = inlineKeyboard {
    if (includeMax) {
      row { dataButton("\uD83C\uDFA5 Дмитрий Максимов", MAXIMOV) }
    }
    if (includeKamen) {
      row { dataButton("\uD83C\uDFA5 Вадим Каменецкий", KAMEN) }
    }
    row { urlButton("\uD83D\uDECD Купить курс", "https://dabromat.ru/start") }
    row {
      urlButton("\uD83D\uDCE2 Подписаться на наш Telegram-канал", "https://t.me/+4brJbkfpd7xhNWE6")
    }
    row {
      urlButton(
        "\uD83C\uDF10 Перейти на сайт",
        "https://dabromat.ru/?utm_source=quest&utm_medium=button&utm_content=link",
      )
    }
    row { urlButton("❓ Задать вопрос", "https://t.me/m/7qK9yUD5YWE6") }
    row { dataButton("\uD83D\uDD19 Назад", RETURN_BACK) }
  }

  fun coursesSelector(availableCourses: List<Pair<Course, Boolean>>) =
    InlineKeyboardMarkup(
      keyboard =
        matrix {
          availableCourses.forEach { (course, status) ->
            val description = if (status) "${course.name} ✅" else course.name
            row { urlButton(description, "https://youtu.be/dQw4w9WgXcQ?si=XOpzfatg17iJuHyt") }
          }
          row { dataButton("Назад", RETURN_BACK) }
        }
    )

  fun back() = InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Назад", RETURN_BACK) } })

  const val YES = "yes"
  const val NO = "no"

  fun confirm() =
    InlineKeyboardMarkup(
      keyboard =
        matrix {
          row {
            dataButton("Да", YES)
            dataButton("Нет", NO)
          }
        }
    )
}
