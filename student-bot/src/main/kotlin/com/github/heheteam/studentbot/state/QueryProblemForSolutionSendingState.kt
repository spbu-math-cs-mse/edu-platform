package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlers
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.FICTITIOUS
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.metaData.buildProblemSendingSelector
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class QueryProblemForSolutionSendingState(
  override val context: User,
  val studentId: StudentId,
  private val selectedCourseId: CourseId,
) : BotStateWithHandlers<Problem?, Unit, StudentApi> { // null means user chose back button
  private val sentMessage = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Problem?, Any>,
  ) {
    updateHandlersController.addTextMessageHandler { message ->
      if (message.content.text == "/menu") {
        NewState(MenuState(context, studentId))
      } else {
        Unhandled
      }
    }
    val assignments = service.getCourseAssignments(selectedCourseId)
    val problems = assignments.associateWith { service.getProblemsFromAssignment(it) }
    val message =
      bot.send(context, Dialogues.askProblem(), replyMarkup = buildProblemSendingSelector(problems))
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

  override fun computeNewState(service: StudentApi, input: Problem?): Pair<State, Unit> {
    return if (input != null) SendSolutionState(context, studentId, input) to Unit
    else {
      MenuState(context, studentId) to Unit
    }
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: StudentApi, response: Unit) {
    for (message in sentMessage) {
      bot.delete(message)
    }
  }

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
