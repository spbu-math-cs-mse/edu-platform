package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.BotState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User

class CheckDeadlinesState(
  override val context: User,
  private val studentId: StudentId,
  private val course: Course,
) : BotState<Unit, Unit, ProblemStorage> {
  override suspend fun readUserInput(bot: BehaviourContext, service: ProblemStorage) {
    val problemsByAssignments = service.getProblemsWithAssignmentsFromCourse(course.id)
    val messageText =
      problemsByAssignments.toList().joinToString("\n\n") { (assignment, problems) ->
        assignment.description +
          "\n" +
          problems.joinToString("\n") { problem ->
            println(problem.deadline)
            "  • ${problem.number} ${problem.deadline?.toString()}"
          }
      }
    bot.sendMessage(context, messageText)
  }

  override fun computeNewState(service: ProblemStorage, input: Unit): Pair<State, Unit> {
    return MenuState(context, studentId) to Unit
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: ProblemStorage,
    response: Unit,
  ) = Unit
}
