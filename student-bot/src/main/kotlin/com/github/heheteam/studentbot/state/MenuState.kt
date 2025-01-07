package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.metaData.ButtonKey
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
  strictlyOn<MenuState> { state ->
    val stickerMessage = bot.sendSticker(state.context.id, Dialogues.typingSticker)
    val menuKeyboard = createMainMenu(state)
    val initialMessage =
      bot.send(state.context, text = Dialogues.menu(), replyMarkup = menuKeyboard.keyboard)

    val datacallbacks =
      waitDataCallbackQueryWithUser(state.context.id).map { callback ->
        val nextState = menuKeyboard.handler(callback.data)
        if (nextState != null) {
          deleteMessage(initialMessage)
          deleteMessage(stickerMessage)
        }
        nextState
      }
    val texts =
      waitTextMessageWithUser(state.context.id).map { t -> handleTextMessage(t, state.context) }
    merge(datacallbacks, texts).firstNotNull()
  }
}

private fun createMainMenu(state: MenuState) =
  buildColumnMenu(
    ButtonData("Записаться на курсы", ButtonKey.SIGN_UP) {
      SignUpState(state.context, state.studentId)
    },
    ButtonData("Посмотреть мои курсы", ButtonKey.VIEW) {
      ViewState(state.context, state.studentId)
    },
    ButtonData("Отправить решение", ButtonKey.SEND_SOLUTION) {
      SendSolutionState(state.context, state.studentId)
    },
    ButtonData("Проверить успеваемость", ButtonKey.CHECK_GRADES) {
      CheckGradesState(state.context, state.studentId)
    },
  )

private suspend fun BehaviourContext.handleTextMessage(
  t: CommonMessage<TextContent>,
  user: User,
): PresetStudentState? {
  val re = Regex("/setid ([0-9]+)")
  val match = re.matchEntire(t.content.text)
  return if (match != null) {
    val newIdStr = match.groups[1]?.value ?: return null
    val newId =
      newIdStr.toLongOrNull()
        ?: run {
          logger.error("input id $newIdStr is not long!")
          return null
        }
    PresetStudentState(user, newId.toStudentId())
  } else {
    bot.sendMessage(user.id, "Unrecognized command")
    null
  }
}
