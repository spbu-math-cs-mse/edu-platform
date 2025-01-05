package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.Dialogues.manyIdsAlreadyExistForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.manyIdsAreGoodForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.manyTeacherIdsDoNotExist
import com.github.heheteam.adminbot.Dialogues.noIdInInput
import com.github.heheteam.adminbot.Dialogues.oneIdAlreadyExistsForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.oneIdIsGoodForTeacherAddition
import com.github.heheteam.adminbot.Dialogues.oneTeacherIdDoesNotExist
import com.github.heheteam.adminbot.processStringIds
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnAddTeacherState(core: AdminCore) {
  strictlyOn<AddTeacherState> { state ->
    send(
      state.context,
    ) {
      +"Введите ID преподавателей (через запятую), которых хотите добавить на курс ${state.courseName}, или отправьте /stop, чтобы отменить операцию."
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
  state: AddTeacherState,
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
  state: AddTeacherState,
  core: AdminCore,
  ids: List<Long>,
): List<Long> {
  val idsThatDoNotExist = processIdsThatDoNotExist(state, core, ids).toSet()
  val idsThatAlreadyExist =
    ids.asSequence().filter { id -> id !in idsThatDoNotExist && core.teachesIn(TeacherId(id), state.course) }.toList()

  if (idsThatAlreadyExist.size == 1) {
    send(
      state.context,
      oneIdAlreadyExistsForTeacherAddition(idsThatAlreadyExist.first(), state.courseName),
    )
  } else if (idsThatAlreadyExist.size > 1) {
    send(
      state.context,
      manyIdsAlreadyExistForTeacherAddition(idsThatAlreadyExist, state.courseName),
    )
  }

  return idsThatDoNotExist.toList() + idsThatAlreadyExist
}

private suspend fun BehaviourContext.processIds(
  state: AddTeacherState,
  core: AdminCore,
  ids: List<Long>,
) {
  val badIds = processBadIds(state, core, ids).toSet()
  val goodIds = ids.asSequence().filter { id -> id !in badIds }.toList()

  if (goodIds.size == 1) {
    send(
      state.context,
      oneIdIsGoodForTeacherAddition(goodIds.first(), state.courseName),
    )
  } else if (goodIds.size > 1) {
    send(
      state.context,
      manyIdsAreGoodForTeacherAddition(goodIds, state.courseName),
    )
  }
  goodIds.forEach { id -> core.registerTeacherForCourse(TeacherId(id), state.course.id) }
}
