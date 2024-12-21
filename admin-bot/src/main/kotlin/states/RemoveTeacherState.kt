package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Dialogues.manyIdsAlreadyDoNotExistForTeacherRemoving
import com.github.heheteam.adminbot.Dialogues.manyIdsAreGoodForTeacherRemoving
import com.github.heheteam.adminbot.Dialogues.manyTeacherIdsDoNotExist
import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.adminbot.Dialogues.oneIdAlreadyDoesNotExistForTeacherRemoving
import com.github.heheteam.adminbot.Dialogues.oneIdIsGoodForTeacherRemoving
import com.github.heheteam.adminbot.Dialogues.oneTeacherIdDoesNotExist
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnRemoveTeacherState(core: AdminCore) {
  strictlyOn<RemoveTeacherState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID преподавателей (через запятую), которых хотите убрать с курса ${state.courseName}, или отправьте /stop, чтобы отменить операцию."
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
      val processedIds = core.processStringIds(splitIds)
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
  state: RemoveTeacherState,
  core: AdminCore,
  ids: List<Long>,
): List<Long> {
  val idsThatDoNotExist = ids.asSequence().filter { id -> !core.teacherExists(TeacherId(id)) }.toList()

  if (idsThatDoNotExist.size == 1) {
    send(
      state.context,
      oneTeacherIdDoesNotExist(idsThatDoNotExist.first()),
    )
  } else if (idsThatDoNotExist.size > 1) {
    send(
      state.context,
      manyTeacherIdsDoNotExist(idsThatDoNotExist),
    )
  }

  return idsThatDoNotExist
}

private suspend fun BehaviourContext.processBadIds(
  state: RemoveTeacherState,
  core: AdminCore,
  ids: List<Long>,
): List<Long> {
  val idsThatDoNotExist = processIdsThatDoNotExist(state, core, ids).toSet()
  val idsThatAlreadyDoNotExist =
    ids.asSequence().filter { id -> id !in idsThatDoNotExist && !core.teachesIn(TeacherId(id), state.course) }.toList()

  if (idsThatAlreadyDoNotExist.size == 1) {
    send(
      state.context,
      oneIdAlreadyDoesNotExistForTeacherRemoving(idsThatAlreadyDoNotExist.first(), state.courseName),
    )
  } else if (idsThatAlreadyDoNotExist.size > 1) {
    send(
      state.context,
      manyIdsAlreadyDoNotExistForTeacherRemoving(idsThatAlreadyDoNotExist, state.courseName),
    )
  }

  return idsThatDoNotExist.toList() + idsThatAlreadyDoNotExist
}

private suspend fun BehaviourContext.processIds(
  state: RemoveTeacherState,
  core: AdminCore,
  ids: List<Long>,
) {
  val badIds = processBadIds(state, core, ids).toSet()
  val goodIds = ids.asSequence().filter { id -> id !in badIds }.toList()

  if (goodIds.size == 1) {
    send(
      state.context,
      oneIdIsGoodForTeacherRemoving(goodIds.first(), state.courseName),
    )
  } else if (goodIds.size > 1) {
    send(
      state.context,
      manyIdsAreGoodForTeacherRemoving(goodIds, state.courseName),
    )
  }
  goodIds.forEach { id -> core.removeTeacher(TeacherId(id), state.course.id) }
}
