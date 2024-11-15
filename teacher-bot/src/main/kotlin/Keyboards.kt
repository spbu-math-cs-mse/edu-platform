package com.github.heheteam.teacherbot
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object Keyboards {
  val parent = "Родитель"
  val other = "Другое"

  fun askGrade() =
    InlineKeyboardMarkup(
      keyboard =
      matrix {
        row {
          dataButton("6", "6")
          dataButton("7", "7")
          dataButton("8", "8")
        }
        row {
          dataButton("9", "9")
          dataButton("10", "10")
          dataButton("11", "11")
        }
        row {
          dataButton("Родитель", parent)
          dataButton("Другое", other)
        }
      },
    )

  val returnBack = "Назад"

  val testSendSolution = "Отправить решение"
  val getSolution = "Получить решение"
  val checkGrades = "checkGrades"
  val viewStats = "Статистика проверок"

  val goodSolution = "Правильное решение"
  val badSolution = "Неправильное решение"

  fun menu() =
    InlineKeyboardMarkup(
      keyboard =
      matrix {
        row {
          dataButton("Получить решение", getSolution)
        }
        row {
          dataButton("Статистика проверок", viewStats)
        }
        row {
          dataButton("Посмотреть успеваемость учеников", checkGrades)
        }
      },
    )

  fun solutionMenu() =
    InlineKeyboardMarkup(
      keyboard =
      matrix {
        row {
          dataButton("\uD83D\uDE80+", goodSolution)
          dataButton("\uD83D\uDE2D-", badSolution)
        }
        row {
          dataButton("Назад \uD83D\uDD19", returnBack)
        }
      },
    )

  fun returnBack() =
    InlineKeyboardMarkup(
      keyboard =
      matrix {
        row {
          dataButton("Назад \uD83D\uDD19", returnBack)
        }
      },
    )
}
