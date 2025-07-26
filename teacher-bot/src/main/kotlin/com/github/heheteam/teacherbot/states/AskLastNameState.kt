package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.state.UpdateHandlersControllerDefault
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.teacherbot.Dialogues
import com.github.heheteam.teacherbot.Keyboards
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class AskLastNameState(override val context: User, private val firstName: String) :
  BotStateWithHandlers<TeacherId, Unit, TeacherApi> {
  override suspend fun computeNewState(
    service: TeacherApi,
    input: TeacherId,
  ): Result<Pair<State, Unit>, FrontendError> = (MenuState(context, input) to Unit).ok()

  override fun defaultState(): State {
    return StartState(context)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherApi,
    response: Unit,
    input: TeacherId,
  ) = Unit.ok()

  override suspend fun outro(bot: BehaviourContext, service: TeacherApi) = Unit

  override suspend fun intro(
    bot: BehaviourContext,
    service: TeacherApi,
    updateHandlersController: UpdateHandlersControllerDefault<TeacherId>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    bot.send(context, Dialogues.askLastName(firstName), replyMarkup = Keyboards.back())
    updateHandlersController.addTextMessageHandler { message ->
      val lastName = message.content.text
      val teacherId = service.createTeacher(firstName, lastName, context.id.chatId.long)
      bot.send(context, Dialogues.niceToMeetYou(firstName, lastName))
      NewState(MenuState(context, teacherId))
    }
    updateHandlersController.addDataCallbackHandler { callback ->
      if (callback.data == Keyboards.RETURN_BACK) {
        NewState(AskFirstNameState(context))
      } else {
        Unhandled
      }
    }
  }
}
