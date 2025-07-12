package com.github.heheteam.parentbot.state

import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.InformationState
import com.github.heheteam.commonlib.state.TextQueryBotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.state.UserInputParsingResult
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.mapBoth
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities

class AddChildById(override val context: User, override val userId: ParentId) :
  TextQueryBotStateWithHandlersAndUserId<ParentApi, ParentId>() {

  override val introMessageContent: TextSourcesList
    get() = buildEntities { +"Введите id ребенка, которого хотите добавить" }

  override fun menuState(): State = Menu(context, userId)

  override fun parseUserTextInput(input: String, service: ParentApi): UserInputParsingResult {
    val id =
      input.toLongOrNull()
        ?: return UserInputParsingResult.Failure(buildEntities { "Введите число" })
    return service
      .addChild(userId, id.toStudentId())
      .mapBoth(
        success = {
          UserInputParsingResult.Success(
            InformationState<ParentApi, ParentId>(
              context,
              userId,
              { TextWithMediaAttachments(succesfullyAddedKidMsg()).ok() },
              menuState(),
            )
          )
        },
        failure = { UserInputParsingResult.Failure(buildEntities { +it.toMessageText() }) },
      )
  }

  private fun succesfullyAddedKidMsg(): TextSourcesList = buildEntities {
    +"Успешно добавлен ребенок"
  }
}
