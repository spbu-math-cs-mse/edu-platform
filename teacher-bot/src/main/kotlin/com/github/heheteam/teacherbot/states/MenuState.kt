package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.michaelbull.result.get
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class MenuState(override val context: User, val teacherId: TeacherId) : State {
  private val messages = mutableListOf<ContentMessage<*>>()
  private val epilogueMessage: String? = null

  suspend fun handle(
    bot: BehaviourContext,
    teacherStorage: TeacherStorage,
    solutionGrader: SolutionGrader,
  ): State =
    with(bot) {
      val input = readUserInput(this, teacherStorage, solutionGrader)
      sendResponse(bot)
      input.first
    }

  suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherStorage,
    solutionGrader: SolutionGrader,
  ): Pair<State, String?> {
    val result = service.updateTgId(teacherId, context.id)
    if (context.username == null) {
      return Pair(StartState(context), null)
    }
    val stickerMessage = bot.sendSticker(context, Dialogues.typingSticker)
    val menuMessage = bot.send(context, Dialogues.menu(), replyMarkup = Keyboards.menu())
    messages.add(stickerMessage)
    messages.add(menuMessage)

    val callbacksFlow =
      bot.waitDataCallbackQueryWithUser(context.id).map { callback ->
        val tryGrading = tryProcessGradingByButtonPress(callback, solutionGrader, teacherId).get()
        println(tryGrading)
        if (tryGrading == null) {
          Pair(handleDataCallbackFromMenuButtons(callback.data), null)
          handleDataCallbackFromMenuButtons(callback.data)?.let { Pair(it, null) }
        } else {
          null
        }
      }
    val messagesFlow =
      bot.waitTextMessageWithUser(context.id).map { message ->
        handleTextMessage(message.content.text)
      }
    return merge(callbacksFlow, messagesFlow).firstNotNull()
  }

  fun computeNewState(
    service: TeacherStorage,
    input: Pair<BotState<*, *, *>, String?>,
  ): Pair<BotState<*, *, *>, String?> {
    return input
  }

  suspend fun sendResponse(bot: TelegramBot) {
    messages.forEach { bot.deleteMessage(context, it.messageId) }
    if (epilogueMessage != null) bot.send(context, epilogueMessage)
  }

  private fun handleTextMessage(message: String): Pair<State, String?> {
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

  private fun handleDataCallbackFromMenuButtons(callback: String): State? =
    when (callback) {
      Keyboards.checkGrades -> CheckGradesState(context, teacherId)
      Keyboards.getSolution -> GettingSolutionState(context, teacherId)
      Keyboards.viewStats -> SendStatisticInfoState(context, teacherId)
      else -> null
    }
}
