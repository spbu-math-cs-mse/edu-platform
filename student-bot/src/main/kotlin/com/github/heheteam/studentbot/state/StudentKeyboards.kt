package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.studentbot.Keyboards
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.InlineKeyboardButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.MatrixBuilder
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object StudentKeyboards {
  const val RETURN_BACK = "returnBack"
  const val ABOUT_COURSE = "aboutCourse"
  const val FREE_ACTIVITY = "freeActivity"
  const val SOLUTIONS = "solutions"
  const val SOLUTION1 = "solution-1"
  const val SOLUTION2 = "solution-2"
  const val SOLUTION3 = "solution-3"
  const val SOLUTION4 = "solution-4"
  const val SOLUTION5 = "solution-5"
  const val SOLUTION6 = "solution-6"
  //  const val SOLUTION7 = "solution-7"
  const val MENU = "menu"
  const val MY_COURSES = "my courses"

  fun menu(selectedCourseId: CourseId?) = inlineKeyboard {
    if (selectedCourseId != null) inlineCourseMenu()
    row { dataButton("\uD83C\uDFAE Квест от Таксы Дуси", FREE_ACTIVITY) }
    row { dataButton("\uD83D\uDCD6 Подробнее о курсе", ABOUT_COURSE) }
    row { urlButton("\uD83D\uDECD Купить курс", "https://dabromat.ru/start") }
    row {
      urlButton("\uD83D\uDCE2 Подписаться на наш Telegram-канал", "https://t.me/+4brJbkfpd7xhNWE6")
    }
    row { dataButton("Разборы квеста", SOLUTIONS) }
    row { dataButton("✍\uFE0F Мои курсы", MY_COURSES) }
  }

  fun solutionMenu() = inlineKeyboard {
    row { dataButton("Первая задача", SOLUTION1) }
    row { dataButton("Вторая задача", SOLUTION2) }
    row { dataButton("Третья задача", SOLUTION3) }
    row { dataButton("Четвертая задача", SOLUTION4) }
    row { dataButton("Пятая задача", SOLUTION5) }
    row { dataButton("Шестая задача", SOLUTION6) }
    //    row { dataButton("Седьмая задача", SOLUTION7) }
    row { dataButton("В главное меню", MENU) }
  }

  const val SEND_SOLUTION = "sendSubmission"
  const val CHECK_GRADES = "checkGrades"
  const val CHECK_DEADLINES = "deadlines"
  const val RESCHEDULE_DEADLINES = "rescheduleDeadlines"
  const val CHALLENGE = "challenge"

  private fun MatrixBuilder<InlineKeyboardButton>.inlineCourseMenu() {
    row { dataButton("Отправить решение", SEND_SOLUTION) }
    row { dataButton("Посмотреть успеваемость", CHECK_GRADES) }
    row { dataButton("Посмотреть дедлайны", CHECK_DEADLINES) }
    row { dataButton("Попросить дорешку", RESCHEDULE_DEADLINES) }
    row { dataButton("Челлендж!", CHALLENGE) }
  }

  fun courseMenu() = inlineKeyboard {
    inlineCourseMenu()
    row { dataButton("Назад", Keyboards.RETURN_BACK) }
  }

  fun defaultKeyboard() = inlineKeyboard {
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

  fun back() = InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Назад", RETURN_BACK) } })

  const val YES = "yes"
  const val NO = "no"
}
