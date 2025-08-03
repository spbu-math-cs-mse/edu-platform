package com.github.heheteam.adminbot.states.assignments

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.state.SimpleState
import com.github.heheteam.commonlib.util.NewState
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.row
import kotlinx.datetime.LocalDateTime

class CompleteAssignmentCreationState(
  override val context: User,
  override val userId: AdminId,
  private val course: Course,
  private var description: Pair<String, LocalDateTime?>,
  private var problems: List<ProblemDescription>,
  private var statementsUrl: String,
) : SimpleState<AdminApi, AdminId>() {

  private val sentMessages = mutableListOf<ContentMessage<TextContent>>()

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }

  override suspend fun BotContext.run(service: AdminApi) {
    service.createAssignment(
      course.id,
      description.first,
      problems.map { it.copy(deadline = description.second) },
      if (statementsUrl == "") null else statementsUrl,
    )
    send(
        Dialogues.assignmentWasCreatedSuccessfully,
        inlineKeyboard { row { dataButton("Отлично!", AdminKeyboards.FICTITIOUS) } },
      )
      .also { sentMessages.add(it) }
    addDataCallbackHandler { NewState(MenuState(context, userId)) }
  }
}
