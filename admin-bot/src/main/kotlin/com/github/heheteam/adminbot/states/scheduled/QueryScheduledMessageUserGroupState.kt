package com.github.heheteam.adminbot.states.scheduled

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.adminbot.states.MenuState
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.logic.UserGroup
import com.github.heheteam.commonlib.state.NavigationBotStateWithHandlers
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
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

class QueryScheduledMessageUserGroupState(
  override val context: User,
  val adminId: AdminId,
  val error: EduPlatformError? = null,
) : NavigationBotStateWithHandlers<AdminApi>() {

  override val introMessageContent: TextSourcesList = buildEntities {
    +Dialogues.queryScheduledMessageUserGroup
  }

  private infix fun <A, B, C> Pair<A, B>.and(third: C): Triple<A, B, C> =
    Triple(first, second, third)

  override fun createKeyboard(service: AdminApi): MenuKeyboardData<State?> {
    val courses = service.getCourses().value
    val groups =
      listOf(
        "Все зарегистрированные пользователи" to "all users" and UserGroup.AllRegisteredUsers,
        "Завершившие квест" to "completed quest" and UserGroup.CompletedQuest,
        "Только админы" to "only admins" and UserGroup.OnlyAdmins,
      ) +
        courses.map { (_, course) ->
          "Ученики \"${course.name}\"" to "course ${course.id}" and UserGroup.CourseGroup(course.id)
        }
    val dateButtons =
      groups.map { (groupName, uniqueData, group) ->
        ButtonData(groupName, uniqueData) {
          QueryScheduledMessageContentState(context, adminId, group) as State
        }
      }

    val cancelButton = ButtonData("Отмена", "cancel") { menuState() }
    return buildColumnMenu(dateButtons + cancelButton)
  }

  override fun menuState(): State = MenuState(context, adminId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<State?>,
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
