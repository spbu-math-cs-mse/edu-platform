package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.commonlib.util.withMessageCleanup
import com.github.heheteam.studentbot.Dialogues
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.flow.first

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnDeveloperStartState(
  studentStorage: StudentStorage
) {
  strictlyOn<DevStartState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val queryIdText = state.queryIdMessage ?: Dialogues.devAskForId()
    withMessageCleanup(bot.send(state.context, queryIdText)) {
      tryQueryAndResolveStudent(state, studentStorage)
    }
  }
}

private suspend fun BehaviourContext.tryQueryAndResolveStudent(
  state: DevStartState,
  studentStorage: StudentStorage,
): BotState {
  val maybeStudent = coroutineBinding {
    val studentIdFromText =
      waitTextMessageWithUser(state.context.id)
        .first()
        .content
        .text
        .toLongOrNull()
        ?.toStudentId()
        .toResultOr { Dialogues.devIdIsNotLong() }
        .bind()
    studentStorage.resolveStudent(studentIdFromText).mapError { Dialogues.devIdNotFound() }.bind()
  }
  return maybeStudent.mapBoth(
    success = { MenuState(state.context, it.id) },
    failure = { errorTryAgainMessage -> DevStartState(state.context, errorTryAgainMessage) },
  )
}
