package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues.manyIdsAlreadyExistForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.manyIdsAreGoodForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.manyTeacherIdsDoNotExist
import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.adminbot.Dialogues.oneIdAlreadyExistsForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.oneIdIsGoodForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.oneTeacherIdDoesNotExist
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
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

class AddTeacherState(
  override val context: User,
  val course: Course,
  val courseName: String,
  val adminId: AdminId,
) : BotStateWithHandlers<String, List<String>, AdminApi> {

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
      bot.send(context) {
        +"Введите ID преподавателей (через запятую), которых хотите добавить на курс $courseName, " +
          "или отправьте /stop, чтобы отменить операцию."
      }

    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
    sentMessages.add(introMessage)
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod") // wild legacy, fix later
  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Result<Pair<State, List<String>>, FrontendError> = coroutineBinding {
    if (input == "/stop") {
      return@coroutineBinding Pair(MenuState(context, adminId), emptyList<String>())
    }

    val splitIds = input.split(",").map { it.trim() }
    if (splitIds.isEmpty()) {
      return@coroutineBinding Pair(this@AddTeacherState, listOf(noIdInInput))
    }

    val processedIds = processStringIds(splitIds)
    if (processedIds.isErr) {
      return@coroutineBinding Pair(this@AddTeacherState, listOf(processedIds.error))
    }

    val ids = processedIds.value
    val messages = mutableListOf<String>()

    // Process invalid IDs
    val badIds = mutableListOf<Long>()
    val goodIds = mutableListOf<Long>()

    // Check non-existent teachers
    val nonExistent = ids.filter { !service.teacherExists(TeacherId(it)) }
    if (nonExistent.isNotEmpty()) {
      messages.add(
        when (nonExistent.size) {
          1 -> oneTeacherIdDoesNotExist(nonExistent.first())
          else -> manyTeacherIdsDoNotExist(nonExistent)
        }
      )
      badIds.addAll(nonExistent)
    }

    // Check already associated teachers
    val alreadyAssociated =
      ids.filter { id -> id !in nonExistent && service.teachesIn(TeacherId(id), course) }
    if (alreadyAssociated.isNotEmpty()) {
      messages.add(
        when (alreadyAssociated.size) {
          1 -> oneIdAlreadyExistsForTeacherAddition(alreadyAssociated.first(), courseName)
          else -> manyIdsAlreadyExistForTeacherAddition(alreadyAssociated, courseName)
        }
      )
      badIds.addAll(alreadyAssociated)
    }

    // Process valid IDs
    goodIds.addAll(ids - badIds.toSet())
    if (goodIds.isNotEmpty()) {
      messages.add(
        when (goodIds.size) {
          1 -> oneIdIsGoodForTeacherAddition(goodIds.first(), courseName)
          else -> manyIdsAreGoodForTeacherAddition(goodIds, courseName)
        }
      )
      goodIds.forEach { service.registerTeacherForCourse(TeacherId(it), course.id) }
    }

    Pair(MenuState(context, adminId), messages)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: List<String>,
    input: String,
  ) = runCatching { response.forEach { msg -> bot.send(context, msg) } }.toTelegramError()

  private fun processStringIds(ids: List<String>): Result<List<Long>, String> {
    return ids
      .map { idStr -> idStr.toLongOrNull()?.let { Ok(it) } ?: Err("Некорректный ID: $idStr") }
      .combine()
      .mapError { errors -> errors }
  }
}
