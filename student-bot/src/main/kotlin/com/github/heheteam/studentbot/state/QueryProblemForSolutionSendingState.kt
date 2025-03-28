package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotStateWithHandlers
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.FICTITIOUS
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.StudentApi
import com.github.heheteam.studentbot.metaData.buildProblemSendingSelector
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage

class QueryProblemForSolutionSendingState(
  override val context: User,
  val studentId: StudentId,
  val selectedCourseId: CourseId,
) : BotStateWithHandlers<Problem?, Unit, StudentApi> { // null means user chose back button
  val sentMessage = mutableListOf<AccessibleMessage>()

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<() -> Unit, Problem?, Any>,
  ) {

    val assignments = service.getCourseAssignments(selectedCourseId)
    val problems = assignments.associateWith { service.getProblemsFromAssignment(it) }
    val message =
      bot.send(context, Dialogues.askProblem(), replyMarkup = buildProblemSendingSelector(problems))
    sentMessage.add(message)
    updateHandlersController.addDataCallbackHandler { dataCallbackQuery ->
      when (val callbackData = dataCallbackQuery.data) {
        FICTITIOUS -> {
          Unhandled
        }
        RETURN_BACK -> {
          UserInput(null)
        }
        else -> {
          val parsedProblem =
            callbackData.split(" ").lastOrNull()?.toLongOrNull()
              ?: return@addDataCallbackHandler Unhandled
          val problem =
            problems.values.flatten().singleOrNull { it.id == ProblemId(parsedProblem) }
              ?: return@addDataCallbackHandler Unhandled
          UserInput(problem)
        }
      }
    }
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
