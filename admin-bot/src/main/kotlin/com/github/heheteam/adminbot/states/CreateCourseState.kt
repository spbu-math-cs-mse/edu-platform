package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.SpreadsheetId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.commonlib.util.toUrl
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class CreateCourseState(override val context: User, val adminId: AdminId) :
  BotStateWithHandlers<String, CreateCourseResponse?, AdminApi> {

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
        "Введите название курса, который хотите создать, или отправьте /stop, чтобы отменить операцию",
      )
    sentMessages.add(introMessage)

    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
  }

  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, CreateCourseResponse?>, FrontendError> {
    val response =
      if (input == "/stop") null
      else {
        response(service, input)
      }
    return Pair(MenuState(context, adminId), response).ok()
  }

  private fun response(service: AdminApi, input: String): CreateCourseResponse {
    val courses =
      service
        .getCourses()
        .mapError {
          return CreateCourseResponse.FailedToLookupCourses
        }
        .value
    val sameNameCourse = courses.map { it.value }.find { it.name == input }
    if (sameNameCourse != null) {
      return CreateCourseResponse.CourseWithNameExists(input, sameNameCourse.id)
    }
    val courseId = service.createCourse(input).value
    return service
      .resolveCourseWithSpreadsheetId(courseId)
      .mapBoth(
        success = { CreateCourseResponse.SuccessfullyCreated(input, it.second) },
        failure = { CreateCourseResponse.FailedToCreateSheet(it) },
      )
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: CreateCourseResponse?,
    input: String,
  ): Result<Unit, FrontendError> =
    runCatching {
        if (response != null) {
          bot.send(context, response.responseMessage)
        }
      }
      .toTelegramError()
}

sealed interface CreateCourseResponse {
  val responseMessage: String

  data class CourseWithNameExists(val name: String, val existingCourse: CourseId) :
    CreateCourseResponse {
    override val responseMessage: String = "Курс с таким названием уже существует"
  }

  data class FailedToCreateCourse(val courseName: String, val error: EduPlatformError) :
    CreateCourseResponse {
    override val responseMessage: String = "Не удалось создать курс $courseName"
  }

  object FailedToLookupCourses : CreateCourseResponse {
    override val responseMessage: String
      get() = "Не получилось посмотреть курсы"
  }

  data class SuccessfullyCreated(val courseName: String, val spreadsheetId: SpreadsheetId) :
    CreateCourseResponse {
    override val responseMessage: String =
      "Курс ${courseName} успешно создан\nРейтинг доступен по ссылке:\n\n${spreadsheetId.toUrl()}"
  }

  data class FailedToCreateSheet(val error: EduPlatformError) : CreateCourseResponse {
    override val responseMessage: String
      get() = "Не получилось создать таблицу, ошибка: ${error.shortDescription}"
  }
}
