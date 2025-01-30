package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStatistics
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

data class CallbacksAndMessages(val callbacks: Flow<String>, val messages: Flow<String>)

class MenuState(override val context: User, val teacherId: TeacherId) :
  BotState<CallbacksAndMessages?, String?, TeacherStatistics> {
  private val messages = mutableListOf<ContentMessage<*>>()
  private val epilogueMessage: String? = null

  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherStatistics,
  ): CallbacksAndMessages? {
    if (context.username == null) {
      return null
    }
    val stickerMessage = bot.sendSticker(context, Dialogues.typingSticker)
    val menuMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())
    messages.add(stickerMessage)
    messages.add(menuMessage)

    val callbacksFlow = bot.waitDataCallbackQueryWithUser(context.id).map { it.data }
    val messagesFlow = bot.waitTextMessageWithUser(context.id).map { it.content.text }
    return CallbacksAndMessages(callbacksFlow, messagesFlow)
  }

  override suspend fun computeNewState(
    service: TeacherStatistics,
    input: CallbacksAndMessages?,
  ): Pair<BotState<*, *, *>, String?> {
    if (input == null) {
      return Pair(StartState(context), null)
    }
    val callbacksResponse =
      input.callbacks.map { callback -> Pair(handleDataCallback(callback), null) }
    val textsResponse = input.messages.map { message -> handleTextMessage(message) }
    return merge(callbacksResponse, textsResponse).firstNotNull()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherStatistics,
    response: String?,
  ) {
    messages.forEach { bot.deleteMessage(context, it.messageId) }
    if (epilogueMessage != null) bot.send(context, epilogueMessage)
  }

  private fun handleTextMessage(message: String): Pair<BotState<*, *, *>, String?> {
    val re = Regex("/setid ([0-9]+)")
    val match = re.matchEntire(message)
    if (match != null) {
      val newIdStr = match.groups[1]?.value ?: return Pair(MenuState(context, teacherId), null)
      val newId =
        newIdStr.toLongOrNull()
          ?: run {
            logger.error("input id $newIdStr is not long!")
            return Pair(MenuState(context, teacherId), null)
          }
      return Pair(PresetTeacherState(context, newId.toTeacherId()), null)
    } else {
      return Pair(MenuState(context, teacherId), "Unrecognized command")
    }
  }

  private fun handleDataCallback(callback: String): BotState<*, *, *> =
    when (callback) {
      Keyboards.checkGrades -> {
        CheckGradesState(context, teacherId)
      }

      Keyboards.getSolution -> {
        GettingSolutionState(context, teacherId)
      }

      Keyboards.viewStats -> {
        SendStatisticInfoState(context, teacherId)
      }

      else -> GettingSolutionState(context, teacherId)
    }
}
