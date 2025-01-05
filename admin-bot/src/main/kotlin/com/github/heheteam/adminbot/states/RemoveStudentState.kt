package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Dialogues.manyIdsAlreadyDoNotExistForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.manyIdsAreGoodForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.manyStudentIdsDoNotExist
import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.adminbot.Dialogues.oneIdAlreadyDoesNotExistForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.oneIdIsGoodForStudentRemoving
import com.github.heheteam.adminbot.Dialogues.oneStudentIdDoesNotExist
import com.github.heheteam.adminbot.processStringIds
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveStudentState(core: AdminCore) {
  strictlyOn<RemoveStudentState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID учеников (через запятую), которых хотите убрать с курса ${state.courseName}, или отправьте /stop, чтобы отменить операцию."
    }
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
  state: RemoveStudentState,
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
  state: RemoveStudentState,
  core: AdminCore,
  ids: List<Long>,
): List<Long> {
  val idsThatDoNotExist = processIdsThatDoNotExist(state, core, ids).toSet()
  val idsThatAlreadyDoNotExist =
    ids.asSequence().filter { id -> id !in idsThatDoNotExist && !core.studiesIn(StudentId(id), state.course) }.toList()

  if (idsThatAlreadyDoNotExist.size == 1) {
    send(
      state.context,
      oneIdAlreadyDoesNotExistForStudentRemoving(idsThatAlreadyDoNotExist.first(), state.courseName),
    )
  } else if (idsThatAlreadyDoNotExist.size > 1) {
    send(
      state.context,
      manyIdsAlreadyDoNotExistForStudentRemoving(idsThatAlreadyDoNotExist, state.courseName),
    )
  }

  return idsThatDoNotExist.toList() + idsThatAlreadyDoNotExist
}

private suspend fun BehaviourContext.processIds(
  state: RemoveStudentState,
  core: AdminCore,
  ids: List<Long>,
) {
  val badIds = processBadIds(state, core, ids).toSet()
  val goodIds = ids.asSequence().filter { id -> id !in badIds }.toList()

  if (goodIds.size == 1) {
    send(
      state.context,
      oneIdIsGoodForStudentRemoving(goodIds.first(), state.courseName),
    )
  } else if (goodIds.size > 1) {
    send(
      state.context,
      manyIdsAreGoodForStudentRemoving(goodIds, state.courseName),
    )
  }
  goodIds.forEach { id -> core.removeStudent(StudentId(id), state.course.id) }
}
