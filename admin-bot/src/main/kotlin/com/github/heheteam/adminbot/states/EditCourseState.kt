package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminKeyboards
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.MenuKeyboardData
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.michaelbull.result.mapBoth
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery

class EditCourseState(
  override val context: User,
  val adminId: AdminId,
  private val course: Course,
) : BotStateWithHandlers<State, Unit, AdminApi> {

  private val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach {
      try {
        bot.delete(it)
      } catch (e: CommonRequestException) {
        KSLog.warning("Failed to delete message", e)
      }
    }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<State>,
  ) {
    val editCourseKeyboard = createEditCourseOptionsKeyboard()

    val message =
      bot.send(context, "Изменить курс ${course.name}:", replyMarkup = editCourseKeyboard.keyboard)
    sentMessages.add(message)

    updateHandlersController.addDataCallbackHandler { callback: DataCallbackQuery ->
      editCourseKeyboard.handler
        .invoke(callback.data)
        .mapBoth(success = { NewState(it) }, failure = { Unhandled })
    }
  }

  private fun createEditCourseOptionsKeyboard(): MenuKeyboardData<State> {
    val editCourseKeyboard =
      buildColumnMenu(
        ButtonData("➕ Добавить учеников", AdminKeyboards.ADD_STUDENT) {
          AddStudentState(context, course, course.name, adminId)
        },
        ButtonData("➖ Убрать учеников", AdminKeyboards.REMOVE_STUDENT) {
          RemoveStudentState(context, course, course.name, adminId)
        },
        ButtonData("➕ Добавить преподавателей", AdminKeyboards.ADD_TEACHER) {
          AddTeacherState(context, course, course.name, adminId)
        },
        ButtonData("➖ Убрать преподавателей", AdminKeyboards.REMOVE_TEACHER) {
          RemoveTeacherState(context, course, course.name, adminId)
        },
        ButtonData("\uD83D\uDD04 Изменить описание", AdminKeyboards.EDIT_DESCRIPTION) {
          EditDescriptionState(context, course, course.name, adminId)
        },
        ButtonData("Создать задание", AdminKeyboards.CREATE_ASSIGNMENT) {
          CreateAssignmentState(context, adminId, course)
        },
        ButtonData("➕ Добавить отложенное сообщение", AdminKeyboards.ADD_SCHEDULED_MESSAGE) {
          AddScheduledMessageStartState(context, course, adminId)
        },
        ButtonData(
          "\uD83D\uDCC3 Просмотреть запланированные сообщения",
          AdminKeyboards.VIEW_SCHEDULED_MESSAGES,
        ) {
          QueryNumberOfRecentMessagesState(context, adminId, course.id)
        },
        ButtonData("❌ Удалить запланированное сообщение", AdminKeyboards.DELETE_SCHEDULED_MESSAGE) {
          QueryMessageIdForDeletionState(context, adminId)
        },
        ButtonData("Назад \uD83D\uDD19", AdminKeyboards.RETURN_BACK) { MenuState(context, adminId) },
      )
    return editCourseKeyboard
  }

  override fun computeNewState(service: AdminApi, input: State): Pair<State, Unit> {
    return Pair(input, Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: Unit,
    input: State,
  ) = Unit
}
