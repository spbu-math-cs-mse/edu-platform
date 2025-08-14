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

  const val RESCHEDULE_DEADLINES = "rescheduleDeadlines"
  const val CHALLENGE = "challenge"

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
