package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.CommonUserId
import com.github.heheteam.commonlib.interfaces.ParentId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.registerStateForBotStateWithHandlersAndUserId
import com.github.heheteam.commonlib.util.UpdateHandlersController
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.types.chat.User

inline fun <
  ApiService : CommonUserApi<UserId>,
  UserId : CommonUserId,
> DefaultBehaviourContextWithFSM<State>.registerQuest(
  studentApi: ApiService,
  noinline initUpdateHandlers:
    (UpdateHandlersController<() -> Unit, out Any?, FrontendError>, User, UserId) -> Unit =
    { _, _, _ ->
    },
) {

  strictlyOn<DefaultErrorState<ApiService, UserId>> { it.handle(this, studentApi) }

  registerStateForBotStateWithHandlersAndUserId<L0<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S0<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S1<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S2<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S3<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<
    L1S3Bellyrub<ApiService, UserId>,
    ApiService,
    UserId,
  >(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<
    L1S4Bellyrub<ApiService, UserId>,
    ApiService,
    UserId,
  >(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L1S4Wrong<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )

  registerStateForBotStateWithHandlersAndUserId<L2S0<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L2Boss<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )

  registerStateForBotStateWithHandlersAndUserId<L3S0<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S1<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<L3S2<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )

  registerStateForBotStateWithHandlersAndUserId<L4Final<ApiService, UserId>, ApiService, UserId>(
    studentApi,
    initUpdateHandlers,
  )
  registerStateForBotStateWithHandlersAndUserId<
    L4Certificate<ApiService, UserId>,
    ApiService,
    UserId,
  >(
    studentApi,
    initUpdateHandlers,
  )
}

inline fun DefaultBehaviourContextWithFSM<State>.registerStudentQuests(
  studentApi: StudentApi,
  noinline initUpdateHandlers:
    (UpdateHandlersController<() -> Unit, out Any?, FrontendError>, User, StudentId) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<DefaultErrorState<StudentApi, StudentId>> {
    it.handle(this, studentApi, initUpdateHandlers)
  }
  strictlyOn<DefaultErrorStateStudent> { it.handle(this, studentApi, initUpdateHandlers) }
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

inline fun DefaultBehaviourContextWithFSM<State>.registerParentQuests(
  parentApi: ParentApi,
  noinline initUpdateHandlers:
    (UpdateHandlersController<() -> Unit, out Any?, FrontendError>, User, ParentId) -> Unit =
    { _, _, _ ->
    },
) {
  strictlyOn<DefaultErrorState<ParentApi, ParentId>> {
    it.handle(this, parentApi, initUpdateHandlers)
  }
  strictlyOn<DefaultErrorStateParent> { it.handle(this, parentApi, initUpdateHandlers) }
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
