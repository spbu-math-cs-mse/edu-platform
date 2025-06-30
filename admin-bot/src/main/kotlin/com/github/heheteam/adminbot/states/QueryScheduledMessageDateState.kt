package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.dateFormatter
import com.github.heheteam.adminbot.toRussian
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities
import java.time.LocalDate

@Suppress("MagicNumber") // working with dates
class QueryScheduledMessageDateState(
  override val context: User,
  val course: Course,
  val adminId: AdminId,
  val scheduledMessageTextField: ScheduledMessageTextField,
  val error: EduPlatformError? = null,
) : NavigationBotStateWithHandlers<AdminApi>() {

  override val introMessageContent: TextSourcesList = buildEntities {
    +Dialogues.queryScheduledMessageDate
  }

  override fun createKeyboard(service: AdminApi): MenuKeyboardData<State?> {
    val today = LocalDate.now()
    val dates = (0..6).map { today.plusDays(it.toLong()) }
    val dateButtons =
      dates.mapIndexed { index, date ->
        val text =
          when (index) {
            0 -> date.format(dateFormatter) + " (сегодня)"
            1 -> date.format(dateFormatter) + " (завтра)"
            else -> date.format(dateFormatter) + " (" + toRussian(date.dayOfWeek) + ")"
          }
        ButtonData(text, date.format(dateFormatter)) {
          QueryScheduledMessageTimeState(context, course, adminId, scheduledMessageTextField, date)
            as State
        }
      }

    val enterManuallyButton =
      ButtonData("Ввести с клавиатуры", "enter date") {
        EnterScheduledMessageDateManuallyState(context, course, adminId, scheduledMessageTextField)
          as State
      }

    val cancelButton = ButtonData("Отмена", "cancel") { menuState() }
    return buildColumnMenu(dateButtons + enterManuallyButton + cancelButton)
  }

  override fun menuState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<State?>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    super.intro(bot, service, updateHandlersController).bind()
    error?.let {
      val errorMessage = bot.send(context, it.shortDescription)
      sentMessages.add(errorMessage)
    }
    updateHandlersController.addTextMessageHandler { message -> handleMessageCallback(message) }
  }

  override fun defaultState(): State = MenuState(context, adminId)

  private fun handleMessageCallback(
    message: CommonMessage<TextContent>
  ): HandlerResultWithUserInputOrUnhandled<SuspendableBotAction, State?, FrontendError> {
    val text = message.content.text
    return if (text == "/stop") {
      NewState(menuState())
    } else {
      Unhandled
    }
  }
}
