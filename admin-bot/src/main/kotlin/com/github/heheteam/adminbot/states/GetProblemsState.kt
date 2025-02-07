package com.github.heheteam.adminbot.states

import com.github.heheteam.adminbot.AssignmentProblemsResolver
import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.util.BotState
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.RegularTextSource
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.utils.RiskFeature

class GetProblemsState(override val context: User, private val course: Course) :
  BotState<Unit, List<Pair<Assignment, List<Problem>>>, AssignmentProblemsResolver> {
  override suspend fun readUserInput(bot: BehaviourContext, service: AssignmentProblemsResolver) =
    Unit

  override fun computeNewState(
    service: AssignmentProblemsResolver,
    input: Unit,
  ): Pair<State, List<Pair<Assignment, List<Problem>>>> =
    Pair(MenuState(context), service.getAssignments(course.id))

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: AssignmentProblemsResolver,
    response: List<Pair<Assignment, List<Problem>>>,
  ) {
    bot.send(context, entities = getProblemsEntitiesList(response))
    MenuState(context)
  }

  @OptIn(RiskFeature::class)
  private fun getProblemsEntitiesList(
    assignmentsList: List<Pair<Assignment, List<Problem>>>
  ): List<TextSource> {
    val noAssignments = "Список серий пуст!"
    return if (assignmentsList.isNotEmpty()) {
      val entitiesList: MutableList<TextSource> = mutableListOf()
      assignmentsList.forEachIndexed { index, (assignment, problemsList) ->
        val noProblems = "Задачи в этой серии отсутствуют."
        val problems =
          if (problemsList.isNotEmpty()) {
            problemsList.joinToString("\n") { problem ->
              "    \uD83C\uDFAF задача ${problem.number}"
            }
          } else {
            noProblems
          }
        entitiesList.addAll(
          listOf(
            RegularTextSource("\uD83D\uDCDA "),
            bold(assignment.description),
            RegularTextSource(
              ":\n$problems${if (index == (assignmentsList.size - 1)) "" else "\n\n"}"
            ),
          )
        )
      }
      entitiesList
    } else {
      listOf(RegularTextSource(noAssignments))
    }
  }
}
