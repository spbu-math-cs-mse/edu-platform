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
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class MenuState(override val context: User, val teacherId: TeacherId) :
  BotState<Pair<BotState<*, *, *>, String?>, String?, TeacherStatistics> {
  private val messages = mutableListOf<ContentMessage<*>>()
  private val epilogueMessage: String? = null

  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherStatistics,
  ): Pair<BotState<*, *, *>, String?> {
    if (context.username == null) {
      return Pair(StartState(context), null)
    }
    val stickerMessage = bot.sendSticker(context, Dialogues.typingSticker)
    val menuMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())
    messages.add(stickerMessage)
    messages.add(menuMessage)

    val callbacksFlow =
      bot.waitDataCallbackQueryWithUser(context.id).map { callback ->
        Pair(handleDataCallback(callback.data), null)
      }
    val messagesFlow =
      bot.waitTextMessageWithUser(context.id).map { message ->
        handleTextMessage(message.content.text)
      }
    return merge(callbacksFlow, messagesFlow).first()
  }

  override fun computeNewState(
    service: TeacherStatistics,
    input: Pair<BotState<*, *, *>, String?>,
  ): Pair<BotState<*, *, *>, String?> {
    return input
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
    return if (match != null) {
      val newId =
        match.groups[1]?.value?.toLongOrNull()
          ?: run {
            logger.error("input id ${match.groups[1]} is not long!")
            return Pair(MenuState(context, teacherId), null)
          }
      Pair(PresetTeacherState(context, newId.toTeacherId()), null)
    } else {
      Pair(MenuState(context, teacherId), "Unrecognized command")
    }
  }

  private fun handleDataCallback(callback: String): BotState<*, *, *> =
    when (callback) {
      Keyboards.checkGrades -> CheckGradesState(context, teacherId)
      Keyboards.getSolution -> GettingSolutionState(context, teacherId)
      Keyboards.viewStats -> SendStatisticInfoState(context, teacherId)
      else -> GettingSolutionState(context, teacherId)
    }
}
