package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues.solutionInfo
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.Keyboards.returnBack
import com.github.heheteam.teacherbot.SolutionAssessor
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge

class GradingSolutionState(
  override val context: User,
  private val teacherId: TeacherId,
  private val solution: Solution,
  private val problem: Problem,
  private val assignment: Assignment,
  private val student: Student,
) : BotState<SolutionAssessment?, Unit, SolutionAssessor> {

  private lateinit var solutionMessage: ContentMessage<*>
  private var markupMessage: ContentMessage<*>? = null
  private val files: MutableList<Pair<MultipartFile, File>> = mutableListOf()

  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: SolutionAssessor,
  ): SolutionAssessment? {
    sendSolution(bot)
    when (
      val response =
        merge(
            bot.waitDataCallbackQueryWithUser(context.id),
            bot.waitTextMessageWithUser(context.id),
          )
          .first()
    ) {
      is DataCallbackQuery -> {
        val command = response.data
        return when (command) {
          Keyboards.goodSolution -> {
            bot.deleteMessage(solutionMessage)
            SolutionAssessment(problem.maxScore, "")
          }
          Keyboards.badSolution -> {
            bot.deleteMessage(solutionMessage)
            SolutionAssessment(0, "")
          }
          returnBack -> {
            bot.delete(solutionMessage)
            null
          }
          else -> null
        }
      }
    }
    return null
  }

  override fun computeNewState(
    service: SolutionAssessor,
    input: SolutionAssessment?,
  ): Pair<BotState<*, *, *>, Unit> {
    val solutionAssessment = input
    if (solutionAssessment != null) {
      service.assessSolution(solution, teacherId, solutionAssessment)
    }
    return Pair(MenuState(context, teacherId), Unit)
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: SolutionAssessor,
    response: Unit,
  ) {
    markupMessage?.let { markupMessage -> bot.delete(markupMessage) }
    files.forEach {
      if (it.second.exists()) {
        it.second.delete()
      }
    }
  }

  private suspend fun sendSolution(bot: BehaviourContext) {
    with(bot) {
      solutionMessage = sendSolutionContent(context.id, solution.attachments)
      markupMessage =
        bot.send(
          context,
          solutionInfo(student, assignment, problem),
          replyMarkup = Keyboards.solutionMenu(),
        )
    }
  }
}
