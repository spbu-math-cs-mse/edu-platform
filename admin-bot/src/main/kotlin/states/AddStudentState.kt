package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Dialogues.manyIdsAlreadyExistForStudentAddition
import com.github.heheteam.adminbot.Dialogues.manyIdsAreGoodForStudentAddition
import com.github.heheteam.adminbot.Dialogues.manyStudentIdsDoNotExist
import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.adminbot.Dialogues.oneIdAlreadyExistsForStudentAddition
import com.github.heheteam.adminbot.Dialogues.oneIdIsGoodForStudentAddition
import com.github.heheteam.adminbot.Dialogues.oneStudentIdDoesNotExist
import com.github.heheteam.adminbot.processStringIds
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddStudentState(core: AdminCore) {
  strictlyOn<AddStudentState> { state ->
    send(
      state.context,
      "Введите ID учеников (через запятую), которых хотите добавить на курс ${state.courseName}, или отправьте /stop, чтобы отменить операцию.",
    )
    val ids: List<Long>
    while (true) {
      val message = waitTextMessageWithUser(state.context.id).first()
      val input = message.content.text
      if (input == "/stop") {
        return@strictlyOn MenuState(state.context)
      }
      val splitIds = input.split(",").map { it.trim() }
      if (splitIds.isEmpty()) {
        send(
          state.context,
          noIdInInput(),
        )
        continue
      }
      val processedIds = processStringIds(splitIds)
      if (processedIds.isErr) {
        send(
          state.context,
          processedIds.error,
        )
        continue
      }
      ids = processedIds.value
      break
    }
    processIds(state, core, ids)
    MenuState(state.context)
  }
}

private suspend fun BehaviourContext.processIdsThatDoNotExist(
  state: AddStudentState,
  core: AdminCore,
  ids: List<Long>,
): List<Long> {
  val idsThatDoNotExist = ids.asSequence().filter { id -> !core.studentExists(StudentId(id)) }.toList()

  if (idsThatDoNotExist.size == 1) {
    send(
      state.context,
      oneStudentIdDoesNotExist(idsThatDoNotExist.first()),
    )
  } else if (idsThatDoNotExist.size > 1) {
    send(
      state.context,
      manyStudentIdsDoNotExist(idsThatDoNotExist),
    )
  }

  return idsThatDoNotExist
}

private suspend fun BehaviourContext.processBadIds(
  state: AddStudentState,
  core: AdminCore,
  ids: List<Long>,
): List<Long> {
  val idsThatDoNotExist = processIdsThatDoNotExist(state, core, ids).toSet()
  val idsThatAlreadyExist =
    ids.asSequence().filter { id -> id !in idsThatDoNotExist && core.studiesIn(StudentId(id), state.course) }.toList()

  if (idsThatAlreadyExist.size == 1) {
    send(
      state.context,
      oneIdAlreadyExistsForStudentAddition(idsThatAlreadyExist.first(), state.courseName),
    )
  } else if (idsThatAlreadyExist.size > 1) {
    send(
      state.context,
      manyIdsAlreadyExistForStudentAddition(idsThatAlreadyExist, state.courseName),
    )
  }

  return idsThatDoNotExist.toList() + idsThatAlreadyExist
}

private suspend fun BehaviourContext.processIds(
  state: AddStudentState,
  core: AdminCore,
  ids: List<Long>,
) {
  val badIds = processBadIds(state, core, ids).toSet()
  val goodIds = ids.asSequence().filter { id -> id !in badIds }.toList()

  if (goodIds.size == 1) {
    send(
      state.context,
      oneIdIsGoodForStudentAddition(goodIds.first(), state.courseName),
    )
  } else if (goodIds.size > 1) {
    send(
      state.context,
      manyIdsAreGoodForStudentAddition(goodIds, state.courseName),
    )
  }
  goodIds.forEach { id -> core.registerStudentForCourse(StudentId(id), state.course.id) }
}
