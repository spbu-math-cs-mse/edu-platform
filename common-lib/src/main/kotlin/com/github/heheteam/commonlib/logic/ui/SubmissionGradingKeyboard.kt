package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.interfaces.SubmissionId
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.utils.matrix
import dev.inmo.tgbotapi.utils.row
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class GradingButtonContent(val submissionId: SubmissionId, val grade: Grade)

internal fun createSubmissionGradingKeyboard(submissionId: SubmissionId) =
  InlineKeyboardMarkup(
    keyboard =
      matrix {
        row {
          dataButton("\uD83D\uDE80+", Json.encodeToString(GradingButtonContent(submissionId, 1)))
          dataButton("\uD83D\uDE2D-", Json.encodeToString(GradingButtonContent(submissionId, 0)))
        }
      }
  )
