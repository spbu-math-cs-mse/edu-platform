package com.github.heheteam.studentbot.metaData

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.util.filterByDeadlineAndSort
import com.github.heheteam.studentbot.Keyboards
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.datetime.LocalDateTime

fun buildProblemSendingSelector(
  availableProblems: Map<Assignment, List<Problem>>,
  currentMoscowTime: LocalDateTime,
  useUrls: Boolean = true,
) =
  InlineKeyboardMarkup(
    keyboard =
      matrix {
        availableProblems.filterByDeadlineAndSort(currentMoscowTime).forEach {
          (assignment, problems) ->
          row {
            val statementsUrl = assignment.statementsUrl
            if (statementsUrl != null && useUrls) {
              urlButton("${assignment.description}:", statementsUrl)
            } else {
              dataButton("${assignment.description}:", Keyboards.FICTITIOUS)
            }
          }
          row {
            problems.forEach { problem ->
              dataButton(problem.number, "${Keyboards.PROBLEM_ID} ${problem.id}")
            }
          }
        }
        row { dataButton("Назад", Keyboards.RETURN_BACK) }
      }
  )

fun back() =
  InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Назад", Keyboards.RETURN_BACK) } })
