package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.metaData.ButtonKey
import com.github.heheteam.studentbot.metaData.menuKeyboard
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
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.StickerContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnMenuState() {
    strictlyOn<MenuState> { state ->
        val stickerMessage =
            bot.sendSticker(state.context.id, Dialogues.typingSticker)
        val initialMessage =
            bot.send(
                state.context,
                text = Dialogues.menu(),
                replyMarkup = menuKeyboard(),
            )

        val datacallbacks =
            waitDataCallbackQueryWithUser(state.context.id).map { callback ->
                handleCallback(initialMessage, stickerMessage, callback, state)
            }
        val texts = waitTextMessageWithUser(state.context.id).map { t ->
            handleTextMessage(t, state.context)
        }
        merge(datacallbacks, texts).firstNotNull()
    }
}

private suspend fun BehaviourContext.handleTextMessage(
    t: CommonMessage<TextContent>,
    user: User,
): PresetStudentState? {
    val re = Regex("/setid ([0-9]+)")
    val match = re.matchEntire(t.content.text)
    return if (match != null) {
        val newIdStr = match.groups[1]?.value ?: return null
        val newId = newIdStr.toLongOrNull() ?: run {
            logger.error("input id $newIdStr is not long!")
            return null
        }
        PresetStudentState(user, newId.toStudentId())
    } else {
        bot.sendMessage(user.id, "Unrecognized command")
        null
    }
}

private suspend fun BehaviourContext.handleCallback(
    initialMessage: ContentMessage<TextContent>,
    stickerMessage: ContentMessage<StickerContent>,
    callback: DataCallbackQuery,
    state: MenuState,
): BotState {
    deleteMessage(initialMessage)
    deleteMessage(stickerMessage)
    return when (callback.data) {
        ButtonKey.VIEW -> ViewState(state.context, state.studentId)
        ButtonKey.SIGN_UP -> SignUpState(state.context, state.studentId)
        ButtonKey.SEND_SOLUTION -> SendSolutionState(
            state.context,
            state.studentId,
        )

        ButtonKey.CHECK_GRADES -> CheckGradesState(
            state.context,
            state.studentId,
        )

        else -> MenuState(state.context, state.studentId)
    }
}
