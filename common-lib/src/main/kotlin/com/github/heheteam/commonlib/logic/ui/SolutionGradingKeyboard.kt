package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.interfaces.SolutionId
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable private data class GradingButtonContent(val solutionId: SolutionId, val grade: Grade)

internal fun createSolutionGradingKeyboard(solutionId: SolutionId) =
  InlineKeyboardMarkup(
    keyboard =
      matrix {
        row {
          dataButton("\uD83D\uDE80+", Json.encodeToString(GradingButtonContent(solutionId, 1)))
          dataButton("\uD83D\uDE2D-", Json.encodeToString(GradingButtonContent(solutionId, 0)))
        }
      }
  )
