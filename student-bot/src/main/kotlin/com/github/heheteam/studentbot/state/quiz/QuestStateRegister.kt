package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.registerStateWithStudentId
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

inline fun DefaultBehaviourContextWithFSM<State>.registerQuest(
  studentApi: StudentApi,
  noinline initUpdateHandlers:
    (UpdateHandlersController<() -> Unit, out Any?, FrontendError>, User, StudentId) -> Unit =
    { _, _, _ ->
    },
) {
  registerStateWithStudentId<L0, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S0, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S1, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S2, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S3, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S3Bellyrub, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S4, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S4Bellyrub, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L1S4Wrong, StudentApi>(studentApi, initUpdateHandlers)

  registerStateWithStudentId<L2S0, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L2Boss, StudentApi>(studentApi, initUpdateHandlers)

  registerStateWithStudentId<L3S0, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L3S1, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L3S2, StudentApi>(studentApi, initUpdateHandlers)

  registerStateWithStudentId<L4Final, StudentApi>(studentApi, initUpdateHandlers)
  registerStateWithStudentId<L4Certificate, StudentApi>(studentApi, initUpdateHandlers)
}
