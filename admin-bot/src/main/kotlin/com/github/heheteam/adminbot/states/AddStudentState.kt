package com.github.heheteam.adminbot.states

import com.github.heheteam.commonlib.api.AdminApi
import com.github.heheteam.adminbot.Dialogues.manyIdsAlreadyExistForStudentAddition
import com.github.heheteam.adminbot.Dialogues.manyIdsAreGoodForStudentAddition
import com.github.heheteam.adminbot.Dialogues.manyStudentIdsDoNotExist
import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.adminbot.Dialogues.oneIdAlreadyExistsForStudentAddition
import com.github.heheteam.adminbot.Dialogues.oneIdIsGoodForStudentAddition
import com.github.heheteam.adminbot.Dialogues.oneStudentIdDoesNotExist
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.combine
import com.github.michaelbull.result.mapError
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class AddStudentState(override val context: User, val course: Course, val courseName: String) :
  BotStateWithHandlers<String, List<String>, AdminApi> {

  val sentMessages = mutableListOf<AccessibleMessage>()

  override suspend fun outro(bot: BehaviourContext, service: AdminApi) {
    // No special cleanup needed
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: AdminApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, String, Any>,
  ) {
    val introMessage =
      bot.send(
        context,
        "Введите ID учеников (через запятую), которых хотите добавить на курс ${course.name}" +
          ", или отправьте /stop, чтобы отменить операцию.",
      )
    sentMessages.add(introMessage)

    updateHandlersController.addTextMessageHandler { message -> UserInput(message.content.text) }
  }

  @Suppress("LongMethod", "CyclomaticComplexMethod") // wild legacy, fix later
  override fun computeNewState(service: AdminApi, input: String): Pair<State, List<String>> {
    if (input == "/stop") {
      return Pair(MenuState(context), emptyList())
    }

    val splitIds = input.split(",").map { it.trim() }
    if (splitIds.isEmpty()) {
      return Pair(this, listOf(noIdInInput()))
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

    val idsThatDoNotExist = ids.filter { !service.studentExists(StudentId(it)) }
    if (idsThatDoNotExist.isNotEmpty()) {
      messages.add(
        when (idsThatDoNotExist.size) {
          1 -> oneStudentIdDoesNotExist(idsThatDoNotExist.first())
          else -> manyStudentIdsDoNotExist(idsThatDoNotExist)
        }
      )
      badIds.addAll(idsThatDoNotExist)
    }

    val idsThatAlreadyExist =
      ids.filter { !idsThatDoNotExist.contains(it) && service.studiesIn(StudentId(it), course) }
    if (idsThatAlreadyExist.isNotEmpty()) {
      messages.add(
        when (idsThatAlreadyExist.size) {
          1 -> oneIdAlreadyExistsForStudentAddition(idsThatAlreadyExist.first(), courseName)
          else -> manyIdsAlreadyExistForStudentAddition(idsThatAlreadyExist, courseName)
        }
      )
      badIds.addAll(idsThatAlreadyExist)
    }

    // Process valid IDs
    goodIds.addAll(ids - badIds.toSet())
    if (goodIds.isNotEmpty()) {
      messages.add(
        when (goodIds.size) {
          1 -> oneIdIsGoodForStudentAddition(goodIds.first(), courseName)
          else -> manyIdsAreGoodForStudentAddition(goodIds, courseName)
        }
      )
      goodIds.forEach { service.registerStudentForCourse(StudentId(it), course.id) }
    }

    return Pair(MenuState(context), messages)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AdminApi,
    response: List<String>,
  ) {
    sentMessages.forEach { bot.delete(it) }
    response.forEach { msg -> bot.send(context, msg) }
  }

  private fun processStringIds(
    ids: List<String>
  ): com.github.michaelbull.result.Result<List<Long>, String> {
    val extractedIds =
      ids.map { idStr -> idStr.toLongOrNull()?.let { Ok(it) } ?: Err("Некорректный ID: $idStr") }
    return extractedIds.combine().mapError { errors -> errors }
  }
}
