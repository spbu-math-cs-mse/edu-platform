package com.github.heheteam.studentbot

import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

object Keyboards {
  const val SEND_SOLUTION = "sendSubmission"
  const val RETURN_BACK = "back"
  const val PROBLEM_ID = "problemId"
  const val CHECK_GRADES = "checkGrades"
  const val FICTITIOUS = "fictitious"
  const val CHECK_DEADLINES = "deadlines"
  const val MOVE_DEADLINES = "moveDeadlines"

  const val COURSES_CATALOG = "coursesCatalog"
  const val PET_THE_DACHSHUND = "petTheeDachshund"
  const val FREE_ACTIVITY = "freeActivity"

  fun menu(isNewUser: Boolean) =
    InlineKeyboardMarkup(
      keyboard =
        matrix {
          if (!isNewUser) {
            row { dataButton("Отправить решение", SEND_SOLUTION) }
            row { dataButton("Проверить успеваемость", CHECK_GRADES) }
            row { dataButton("Посмотреть дедлайны", CHECK_DEADLINES) }
            row { dataButton("Запросить дорешку", MOVE_DEADLINES) }
          }
          row { dataButton("Каталог курсов", COURSES_CATALOG) }
          row { dataButton("Почесать Таксе пузо", PET_THE_DACHSHUND) }
          row { dataButton("Квест от Таксы Дуси", FREE_ACTIVITY) }
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
