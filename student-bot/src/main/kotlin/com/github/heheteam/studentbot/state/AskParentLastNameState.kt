package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.ParentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlerManager
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class AskParentLastNameState(
  override val context: User,
  private val firstName: String,
  private val from: String?,
) : BotStateWithHandlers<StudentId, Unit, ParentApi> {
  override suspend fun computeNewState(
    service: ParentApi,
    input: StudentId,
  ): Result<Pair<State, Unit>, FrontendError> = (MenuState(context, input) to Unit).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ParentApi,
    response: Unit,
    input: StudentId,
  ) = Unit.ok()

  override fun defaultState(): State {
    return SelectStudentParentState(context, from)
  }

  override suspend fun outro(bot: BehaviourContext, service: ParentApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: ParentApi,
    updateHandlersController: UpdateHandlerManager<StudentId>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    bot.send(context, Dialogues.askLastName(firstName), replyMarkup = Keyboards.back())
    updateHandlersController.addTextMessageHandler { message ->
      val lastName = message.content.text
      NewState(SelectParentGradeState(context, firstName, lastName, from))
    }

    updateHandlersController.addDataCallbackHandler { callBack ->
      if (callBack.data == Keyboards.RETURN_BACK) {
        NewState(SelectStudentParentState(context, from))
      } else {
        Unhandled
      }
    }
  }
}
