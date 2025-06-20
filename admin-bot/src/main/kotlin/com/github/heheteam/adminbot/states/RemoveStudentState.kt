package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.Dialogues.manyIdsAlreadyDoNotExistForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.manyIdsAreGoodForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.manyStudentIdsDoNotExist
import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.adminbot.Dialogues.oneIdAlreadyDoesNotExistForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.oneIdIsGoodForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.oneStudentIdDoesNotExist
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class RemoveStudentState(
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
    updateHandlersController: UpdateHandlerManager<String>,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val message =
      bot.send(context) {
        +"Введите ID учеников (через запятую), которых хотите убрать с курса $courseName, " +
          "или отправьте /stop, чтобы отменить операцию."
      }
    sentMessages.add(message)

    updateHandlersController.addTextMessageHandler { textMessage ->
      UserInput(textMessage.content.text)
    }
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod") // wild legacy, fix later
  override suspend fun computeNewState(
    service: AdminApi,
    input: String,
  ): Pair<State, List<String>> {
    if (input == "/stop") {
      return Pair(MenuState(context, adminId), emptyList())
    }

    val splitIds = input.split(",").map { it.trim() }
    if (splitIds.isEmpty()) {
      return Pair(this, listOf(noIdInInput))
    }

    val processedIds = processStringIds(splitIds)
    if (processedIds.isErr) {
      return Pair(this, listOf(processedIds.error))
    }

    val ids = processedIds.value
    val messages = mutableListOf<String>()

    // Process invalid IDs
    val badIds = mutableListOf<Long>()
    val goodIds = mutableListOf<Long>()

    // Check non-existent students
    val nonExistent = ids.filter { !service.studentExists(StudentId(it)) }
    if (nonExistent.isNotEmpty()) {
      messages.add(
        when (nonExistent.size) {
          1 -> oneStudentIdDoesNotExist(nonExistent.first())
          else -> manyStudentIdsDoNotExist(nonExistent)
        }
      )
      badIds.addAll(nonExistent)
    }

    // Check students not enrolled
    val notEnrolled =
      ids.filter { id -> id !in nonExistent && !service.studiesIn(StudentId(id), course) }
    if (notEnrolled.isNotEmpty()) {
      messages.add(
        when (notEnrolled.size) {
          1 -> oneIdAlreadyDoesNotExistForStudentRemoving(notEnrolled.first(), courseName)
          else -> manyIdsAlreadyDoNotExistForStudentRemoving(notEnrolled, courseName)
        }
      )
      badIds.addAll(notEnrolled)
    }

    // Process valid IDs
    goodIds.addAll(ids - badIds.toSet())
    if (goodIds.isNotEmpty()) {
      messages.add(
        when (goodIds.size) {
          1 -> oneIdIsGoodForStudentRemoving(goodIds.first(), courseName)
          else -> manyIdsAreGoodForStudentRemoving(goodIds, courseName)
        }
      )
      goodIds.forEach { service.removeStudent(StudentId(it), course.id) }
    }

    return Pair(MenuState(context, adminId), messages)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: List<String>,
    input: String,
  ) {
    response.forEach { msg -> bot.send(context, msg) }
  }

  private fun processStringIds(ids: List<String>): Result<List<Long>, String> {
    return ids
      .map { idStr -> idStr.toLongOrNull()?.let { Ok(it) } ?: Err("Некорректный ID: $idStr") }
      .combine()
  }
}
