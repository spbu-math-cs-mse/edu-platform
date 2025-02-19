package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.DeleteMessageAction
import com.github.heheteam.commonlib.util.HandlerResult
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.queryCourse
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.heheteam.studentbot.Keyboards.CHECK_DEADLINES
import com.github.heheteam.studentbot.Keyboards.CHECK_GRADES
import com.github.heheteam.studentbot.Keyboards.SEND_SOLUTION
import com.github.heheteam.studentbot.Keyboards.SIGN_UP
import com.github.heheteam.studentbot.Keyboards.VIEW
import com.github.michaelbull.result.get
import com.github.michaelbull.result.runCatching
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.coroutines.firstNotNull
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class MenuState(override val context: User, val studentId: StudentId) :
  BotState<State, Unit, CoursesDistributor> {

  override suspend fun handle(bot: BehaviourContext, service: CoursesDistributor): State {
    val stickerMessage = bot.sendSticker(context.id, Dialogues.typingSticker)
    val initialMessage = bot.send(context, text = Dialogues.menu(), replyMarkup = Keyboards.menu())

    val extractMenuKeypress: suspend (DataCallbackQuery) -> HandlerResult.UserInput<State>? =
      { callback ->
        extractMenuKeypressFrom(callback, bot, service)?.let { newState ->
          bot.delete(initialMessage, stickerMessage)
          HandlerResult.UserInput(newState)
        }
      }
    val extractDeleteCommandHandler = { callback: DataCallbackQuery ->
      val result = runCatching { Json.decodeFromString<DeleteMessageAction>(callback.data) }.get()
      if (result != null) {
        HandlerResult.Action({ _, _ -> deleteMessage(result.chatId, result.messageId) })
      } else {
        null
      }
    }
    val dataCallbacks =
      bot
        .waitDataCallbackQueryWithUser(context.id)
        .map { dataCallback: DataCallbackQuery ->
          val deleteMessageAction = extractDeleteCommandHandler(dataCallback)
          if (deleteMessageAction != null) {
            deleteMessageAction.action.invoke(bot, context, context.id)
            null
          } else {
            extractMenuKeypress(dataCallback)
          }
        }
        .firstNotNull()

    val userInput = dataCallbacks.value
    val (nextState, response) = computeNewState(service, userInput)
    sendResponse(bot, service, response)
    return nextState

    //    temporary comments; shall be enabled again
    //    val texts =
    //      bot.waitTextMessageWithUser(context.id).mapNotNull { t -> bot.handleTextMessage(t,
    // context) }
    //    val newState = merge(dataCallbacks, texts).first()
    //    return newState
  }

  override suspend fun readUserInput(bot: BehaviourContext, service: CoursesDistributor): State {
    TODO("Shall not be called at all")
  }

  private suspend fun extractMenuKeypressFrom(
    callback: DataCallbackQuery,
    bot: BehaviourContext,
    service: CoursesDistributor,
  ): State? =
    when (callback.data) {
      SIGN_UP -> {
        SignUpState(context, studentId)
      }

      VIEW -> {
        ViewState(context, studentId)
      }

      SEND_SOLUTION -> {
        val courses = service.getStudentCourses(studentId)
        bot.queryCourse(context, courses)?.let { SendSolutionState(context, studentId, it) }
      }

      CHECK_GRADES -> {
        CheckGradesState(context, studentId)
      }

      CHECK_DEADLINES -> {
        val courses = service.getStudentCourses(studentId)
        bot.queryCourse(context, courses)?.let { CheckDeadlinesState(context, studentId, it) }
      }

      else -> null
    }

  override fun computeNewState(service: CoursesDistributor, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: CoursesDistributor,
    response: Unit,
  ) = Unit

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
}
