package com.github.heheteam.studentbot.metaData

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.util.filterByDeadlineAndSort
import com.github.heheteam.studentbot.Keyboards
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row

fun buildProblemSendingSelector(availableProblems: Map<Assignment, List<Problem>>) =
  InlineKeyboardMarkup(
    keyboard =
      matrix {
        availableProblems.filterByDeadlineAndSort().forEach { (assignment, problems) ->
          row { dataButton("${assignment.description}:", Keyboards.FICTITIOUS) }
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
