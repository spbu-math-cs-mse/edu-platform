package com.github.heheteam.studentbot

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
