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
import kotlin.math.ceil
import kotlinx.datetime.LocalDateTime

private const val MAX_LENGTH_OF_KEYBOARD_ROW: Float = 10f

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
              urlButton("${assignment.name}:", statementsUrl)
            } else {
              dataButton("${assignment.name}:", Keyboards.FICTITIOUS)
            }
          }
          val numberOfRows = ceil(problems.size.toFloat() / MAX_LENGTH_OF_KEYBOARD_ROW)
          val rowLength = ceil(problems.size.toFloat() / numberOfRows).toInt()
          problems.chunked(rowLength).forEach { problemsRow ->
            row {
              problemsRow.forEach { problem ->
                dataButton(problem.number, "${Keyboards.PROBLEM_ID} ${problem.id}")
              }
            }
          }
        }
        row { dataButton("Назад", Keyboards.RETURN_BACK) }
      }
  )

fun back() =
  InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Назад", Keyboards.RETURN_BACK) } })
