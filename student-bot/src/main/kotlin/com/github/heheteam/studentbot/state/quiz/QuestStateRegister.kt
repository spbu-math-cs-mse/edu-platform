package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.state.registerStateForBotStateWithHandlersAndUserId
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

@Suppress("LongMethod") // fixed when we introduce registering by subclassing
inline fun DefaultBehaviourContextWithFSM<State>.registerStudentQuests(
  studentApi: StudentApi,
  noinline initUpdateHandlers:
    (UpdateHandlersControllerDefault<out Any?>, User, StudentId) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<DefaultErrorStateStudent> { it.handleWithIds(this, studentApi, initUpdateHandlers) }
  registerStateForBotStateWithHandlersAndUserId<L0Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S0Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S1Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S2Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S3Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S3BellyrubStudent, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4BellyrubStudent, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4WrongStudent, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L2S0Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L2BossStudent, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S0Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S1Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S2Student, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L4FinalStudent, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L4CertificateStudent, StudentApi, StudentId>(
    studentApi,
    initUpdateHandlers,
  )
}

@Suppress("LongMethod") // fixed when we introduce registering by subclassing
inline fun DefaultBehaviourContextWithFSM<State>.registerParentQuests(
  parentApi: ParentApi,
  noinline initUpdateHandlers: (UpdateHandlersControllerDefault<out Any?>, User, ParentId) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<DefaultErrorStateParent> { it.handleWithIds(this, parentApi, initUpdateHandlers) }
  registerStateForBotStateWithHandlersAndUserId<L0Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S0Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S1Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S2Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S3Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S3BellyrubParent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4BellyrubParent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4WrongParent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L2S0Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L2BossParent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S0Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S1Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S2Parent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L4FinalParent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L4CertificateParent, ParentApi, ParentId>(
    parentApi,
    initUpdateHandlers,
  )
}
