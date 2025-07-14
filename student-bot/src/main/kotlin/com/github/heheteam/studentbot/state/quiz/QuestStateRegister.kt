package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.api.CommonUserApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.CommonUserId
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
