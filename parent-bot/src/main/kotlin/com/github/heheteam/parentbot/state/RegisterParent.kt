package com.github.heheteam.parentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.state.TextQueryBotStateWithHandlers
import com.github.heheteam.commonlib.state.UserInputParsingResult
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class RegisterParent(override val context: User) : TextQueryBotStateWithHandlers<ParentApi>() {
  override val introMessageContent: TextSourcesList
    get() = buildEntities { +"Введите ваши имя и фамилию через пробел" }

  override fun menuState(): State = Start(context)

  override fun parseUserTextInput(input: String, service: ParentApi): UserInputParsingResult {
    val parts = (input.split(" ").map { it.trim() })
    return if (parts.size != 2) {
      UserInputParsingResult.Failure(buildEntities { "Вы ввели ${parts.size} через пробел" })
    } else {
      val (firstName, lastName) = parts
      service
        .createParent(firstName, lastName, context.id.chatId)
        .mapBoth(
          success = { parent -> UserInputParsingResult.Success(Menu(context, parent.id)) },
          failure = { error ->
            UserInputParsingResult.Failure(buildEntities { +error.toMessageText() })
          },
        )
    }
  }
}
