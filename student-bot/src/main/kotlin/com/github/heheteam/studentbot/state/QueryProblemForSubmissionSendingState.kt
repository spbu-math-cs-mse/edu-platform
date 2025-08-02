package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlerManager
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.getCurrentMoscowTime
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.FICTITIOUS
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.metaData.buildProblemSendingSelector
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class QueryProblemForSubmissionSendingState(
  override val context: User,
  override val userId: StudentId,
  private val selectedCourseId: CourseId,
) :
  BotStateWithHandlersAndStudentId<
    Problem?,
    Unit,
    StudentApi,
  > { // null means user chose back button
  private val sentMessage = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlerManager<Problem?>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val problems = service.getActiveProblems(userId, selectedCourseId).bind()
    val message =
      bot.send(
        context,
        Dialogues.askProblem,
        replyMarkup = buildProblemSendingSelector(problems, getCurrentMoscowTime()),
      )
    sentMessage.add(message)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      when (val callbackData = dataCallbackQuery.data) {
        FICTITIOUS -> Unhandled
        RETURN_BACK -> UserInput(null)
        else -> parseProblemFromDataCallbackQuery(callbackData, problems)
      }
    }
  }

  private fun parseProblemFromDataCallbackQuery(
    callbackData: String,
    problems: Map<Assignment, List<Problem>>,
  ): HandlerResultWithUserInputOrUnhandled<Nothing, Problem, Nothing> {
    val maybeParsedProblem =
      callbackData.split(" ").lastOrNull()?.toLongOrNull()?.let { ProblemId(it) }
    val problem =
      problems.values.flatten().singleOrNull { it.id == maybeParsedProblem } ?: return Unhandled
    return UserInput(problem)
  }

  override suspend fun computeNewState(
    service: StudentApi,
    input: Problem?,
  ): Result<Pair<State, Unit>, FrontendError> {
    return if (input != null) SendSubmissionState(context, userId, input) to Unit
      else {
        MenuState(context, userId) to Unit
      }
      .ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
    input: Problem?,
  ): Result<Unit, FrontendError> =
    runCatching {
        for (message in sentMessage) {
          bot.delete(message)
        }
      }
      .toTelegramError()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
