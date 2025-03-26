package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.queryCourse
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.heheteam.studentbot.Keyboards.CHECK_DEADLINES
import com.github.heheteam.studentbot.Keyboards.CHECK_GRADES
import com.github.heheteam.studentbot.Keyboards.SEND_SOLUTION
import com.github.heheteam.studentbot.Keyboards.SIGN_UP
import com.github.heheteam.studentbot.Keyboards.VIEW
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

data class MenuState(override val context: User, val studentId: StudentId) :
  BotState<State, Unit, CoursesDistributor> {
  override suspend fun readUserInput(bot: BehaviourContext, service: CoursesDistributor): State {
    val stickerMessage = bot.sendSticker(context.id, Dialogues.typingSticker)
    val initialMessage = bot.send(context, text = Dialogues.menu(), replyMarkup = Keyboards.menu())

    val dataCallbacks =
      bot.waitDataCallbackQueryWithUser(context.id).mapNotNull { callback ->
        when (callback.data) {
          SIGN_UP -> {
            bot.delete(stickerMessage, initialMessage)
            SignUpState(context, studentId)
          }

          VIEW -> {
            bot.delete(stickerMessage, initialMessage)
            ViewState(context, studentId)
          }

          SEND_SOLUTION -> {
            bot.delete(stickerMessage, initialMessage)
            QueryCourseForSolutionSendingState(context, studentId)
          }

          CHECK_GRADES -> {
            bot.delete(stickerMessage, initialMessage)
            CheckGradesState(context, studentId)
          }

          CHECK_DEADLINES -> {
            bot.delete(stickerMessage, initialMessage)
            val courses = service.getStudentCourses(studentId)
            bot.queryCourse(context, courses)?.let { CheckDeadlinesState(context, studentId, it) }
          }

          else -> null
        }
      }

    val texts =
      bot.waitTextMessageWithUser(context.id).mapNotNull { t -> bot.handleTextMessage(t, context) }
    val newState = merge(dataCallbacks, texts).first()
    return newState
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
