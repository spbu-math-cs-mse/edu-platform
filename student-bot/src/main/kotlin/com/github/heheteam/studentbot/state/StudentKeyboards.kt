package com.github.heheteam.studentbot.state

import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object StudentKeyboards {
  const val RETURN_BACK = "returnBack"
  const val ABOUT_COURSE = "aboutCourse"
  const val FREE_ACTIVITY = "freeActivity"
  const val SOLUTIONS = "solutions"
  const val FIRST_SOLUTION = "solution-1"
  const val MENU = "menu"
  const val MY_COURSES = "my courses"

  fun menu() = inlineKeyboard {
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
    row { dataButton("Первая задача", FIRST_SOLUTION) }
    row { dataButton("В главное меню", MENU) }
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
