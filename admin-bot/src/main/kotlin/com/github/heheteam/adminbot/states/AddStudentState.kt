package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.domain.AddStudentStatus
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.toStudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
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

sealed interface AddStudentServiceResult {
  data class ParserError(val messageToDisplay: String) : AddStudentServiceResult

  data class ParserSuccess(val result: List<Pair<StudentId, AddStudentStatus>>) :
    AddStudentServiceResult
}

class AddStudentState(
  override val context: User,
  val course: Course,
  val courseName: String,
  val adminId: AdminId,
) : BotStateWithHandlers<String, AddStudentServiceResult, AdminApi> {

  val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, adminId)

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    sentMessages.forEach { bot.delete(it) }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val introMessage =
      bot.send(
        context,
        "Введите ID учеников (через запятую), которых хотите добавить на курс ${course.name}" +
          ", или отправьте /stop, чтобы отменить операцию.",
      )

    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
    sentMessages.add(introMessage)
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod") // wild legacy, fix later
  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, AddStudentServiceResult>, FrontendError> = coroutineBinding {
    val splitIds = input.split(",").map { it.trim() }
    if (splitIds.isEmpty()) {
      return@coroutineBinding Pair(
        this@AddStudentState,
        AddStudentServiceResult.ParserError(noIdInInput),
      )
    }

    val studentIds =
      stringsToInts(splitIds)
        .ensureSuccess {
          return@coroutineBinding Pair(
            this@AddStudentState,
            AddStudentServiceResult.ParserError(it),
          )
        }
        .map { it.toStudentId() }
    val result = studentIds.zip(service.addStudents(course.id, studentIds).bind())

    Pair(MenuState(context, adminId), AddStudentServiceResult.ParserSuccess(result))
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: AddStudentServiceResult,
    input: String,
  ): Result<Unit, FrontendError> =
    runCatching {
        when (response) {
          is AddStudentServiceResult.ParserError -> {
            bot.send(context, response.messageToDisplay)
          }
          is AddStudentServiceResult.ParserSuccess -> {
            val studentsStatuses = response.result
            val statusMessage = createStatusMessage(studentsStatuses)
            bot.send(context, statusMessage)
          }
        }
        Unit
      }
      .toTelegramError()

  private fun createStatusMessage(
    studentsStatuses: List<Pair<StudentId, AddStudentStatus>>
  ): TextSourcesList = buildEntities {
    regularln("Статус добавления учеников:")
    studentsStatuses.map { (studentId, status) ->
      val stringStatus =
        when (status) {
          AddStudentStatus.Exists -> "уже существует"
          AddStudentStatus.Success -> "успешно добавлен"
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
