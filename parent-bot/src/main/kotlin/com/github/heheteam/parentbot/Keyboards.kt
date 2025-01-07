package com.github.heheteam.parentbot

import com.github.heheteam.commonlib.Student
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.urlButton
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
        }
    )

  val petDog = "Почесать таксе пузо"
  val giveFeedback = "Дать обратную связь"
  val returnBack = "Назад"

  fun menu(children: List<Student>) =
    InlineKeyboardMarkup(
      keyboard =
        matrix {
          row { dataButton("Почесать таксе пузо", petDog) }
          row {
            urlButton("Учиться на курсах", "https://dabromat.ru/#rec784116042")
            dataButton("Дать обратную связь", giveFeedback)
          }
          if (children.isNotEmpty()) {
            row { dataButton("Следить за успеваемостью \uD83D\uDC47", "-") }
            for (child in children) {
              row { dataButton(child.toString(), child.id.toString()) }
            }
          }
        }
    )

  fun returnBack() =
    InlineKeyboardMarkup(keyboard = matrix { row { dataButton("Назад \uD83D\uDD19", returnBack) } })
}
