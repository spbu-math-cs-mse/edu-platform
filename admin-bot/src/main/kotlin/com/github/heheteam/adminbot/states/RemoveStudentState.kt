package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.domain.RemoveStudentStatus
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ensureSuccess
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.regularln

sealed interface RemoveStudentServiceResult {
  data class ParserError(val messageToDisplay: String) : RemoveStudentServiceResult

  data class ParserSuccess(val result: List<Pair<StudentId, RemoveStudentStatus>>) :
    RemoveStudentServiceResult
}

class RemoveStudentState(
  override val context: User,
  val course: Course,
  val courseName: String,
  val adminId: AdminId,
) : BotStateWithHandlers<String, RemoveStudentServiceResult, AdminApi> {

  val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach { bot.delete(it) }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersControllerDefault<String>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val introMessage =
      bot.send(
        context,
        "Введите ID учеников (через запятую), которых хотите убрать с курса $courseName, " +
          "или отправьте /stop, чтобы отменить операцию.",
      )

    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
    sentMessages.add(introMessage)
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, RemoveStudentServiceResult>, FrontendError> = coroutineBinding {
    val splitIds = input.split(",").map { it.trim() }
    if (splitIds.isEmpty()) {
      return@coroutineBinding Pair(
        this@RemoveStudentState,
        RemoveStudentServiceResult.ParserError(noIdInInput),
      )
    }

    val studentIds =
      stringsToInts(splitIds)
        .ensureSuccess {
          return@coroutineBinding Pair(
            this@RemoveStudentState,
            RemoveStudentServiceResult.ParserError(it),
          )
        }
        .map { it.toStudentId() }
    val result = studentIds.zip(service.removeStudents(course.id, studentIds).bind())

    Pair(MenuState(context, adminId), RemoveStudentServiceResult.ParserSuccess(result))
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: RemoveStudentServiceResult,
    input: String,
  ): Result<Unit, FrontendError> =
    runCatching {
        when (response) {
          is RemoveStudentServiceResult.ParserError -> {
            bot.send(context, response.messageToDisplay)
          }
          is RemoveStudentServiceResult.ParserSuccess -> {
            val studentsStatuses = response.result
            val statusMessage = createStatusMessage(studentsStatuses)
            bot.send(context, statusMessage)
          }
        }
        Unit
      }
      .toTelegramError()

  private fun createStatusMessage(
    studentsStatuses: List<Pair<StudentId, RemoveStudentStatus>>
  ): TextSourcesList = buildEntities {
    regularln("Статус удаления учеников:")
    studentsStatuses.map { (studentId, status) ->
      val stringStatus =
        when (status) {
          RemoveStudentStatus.Removed -> "успешно удален"
          RemoveStudentStatus.NotFoundInCourse -> "не найден"
          RemoveStudentStatus.NotAStudent -> "не существует в базе данных"
        }
      regularln("$studentId -- $stringStatus")
    }
  }

  private fun stringsToInts(ids: List<String>): Result<List<Long>, String> {
    val extractedIds =
      ids.map { idStr -> idStr.toLongOrNull()?.let { Ok(it) } ?: Err("Некорректный ID: $idStr") }
    return extractedIds.combine().mapError { errors -> errors }
  }
}
