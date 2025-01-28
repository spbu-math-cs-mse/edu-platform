package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.Dialogues.solutionInfo
import com.github.heheteam.teacherbot.Keyboards
import com.github.heheteam.teacherbot.Keyboards.returnBack
import com.github.heheteam.teacherbot.TeacherCore
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

// TODO: Replace TeacherCore with a new, more convenient service
class CheckingSolutionState(
  override val context: User,
  private val teacherId: TeacherId,
  private val solution: Solution,
  private val problem: Problem,
  private val assignment: Assignment,
  private val student: Student,
) : BotState<SolutionAssessment?, Unit, TeacherCore> {

  private lateinit var solutionMessage: ContentMessage<*>
  private var markupMessage: ContentMessage<*>? = null
  private val files: MutableList<Pair<MultipartFile, File>> = mutableListOf()

  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: TeacherCore,
  ): SolutionAssessment? {
    sendSolution(bot)

    when (
      val response =
        flowOf(
            bot.waitDataCallbackQueryWithUser(context.id),
            bot.waitTextMessageWithUser(context.id),
          )
          .flattenMerge()
          .first()
    ) {
      is DataCallbackQuery -> {
        val command = response.data
        when (command) {
          Keyboards.goodSolution -> {
            bot.deleteMessage(solutionMessage)
            return SolutionAssessment(problem.maxScore, "")
          }

          Keyboards.badSolution -> {
            bot.deleteMessage(solutionMessage)
            return SolutionAssessment(0, "")
          }

          returnBack -> {
            bot.delete(solutionMessage)
            return null
          }
        }
      }
    }
    return null
  }

  override suspend fun computeNewState(
    service: TeacherCore,
    solutionAssessment: SolutionAssessment?,
  ): Pair<BotState<*, *, *>, Unit> {
    if (solutionAssessment != null) {
      service.assessSolution(solution, teacherId, solutionAssessment)
    }
    return Pair(MenuState(context, teacherId), Unit)
  }

  override suspend fun sendResponse(bot: BehaviourContext, service: TeacherCore, response: Unit) {
    if (markupMessage != null) {
      bot.delete(markupMessage!!)
    }
    files.forEach {
      if (it.second.exists()) {
        it.second.delete()
      }
    }
  }

  @OptIn(RiskFeature::class)
  private suspend fun sendSolution(bot: BehaviourContext) {
    when (solution.content.type!!) {
      SolutionType.TEXT ->
        solutionMessage =
          bot.send(
            context,
            solution.content.text!! + "\n\n\n" + solutionInfo(student, assignment, problem),
            replyMarkup = Keyboards.solutionMenu(),
          )

      SolutionType.PHOTO -> {
        files.add(SolutionProvider.getSolutionWithURL(solution.content.filesURL!!.first()))
        solutionMessage =
          bot.sendPhoto(
            context,
            files.first().first,
            text =
              if (solution.content.text == null) {
                solutionInfo(student, assignment, problem)
              } else {
                solution.content.text + "\n\n\n" + solutionInfo(student, assignment, problem)
              },
            replyMarkup = Keyboards.solutionMenu(),
          )
      }

      SolutionType.DOCUMENT -> {
        files.add(SolutionProvider.getSolutionWithURL(solution.content.filesURL!!.first()))
        solutionMessage =
          bot.sendDocument(
            context,
            files.first().first,
            text =
              if (solution.content.text == null) {
                solutionInfo(student, assignment, problem)
              } else {
                solution.content.text + "\n\n\n" + solutionInfo(student, assignment, problem)
              },
            replyMarkup = Keyboards.solutionMenu(),
          )
      }

      SolutionType.GROUP -> {
        files.addAll(solution.content.filesURL!!.map { SolutionProvider.getSolutionWithURL(it) })
        solutionMessage = bot.sendMediaGroup(context, files.map { TelegramMediaDocument(it.first) })
        markupMessage =
          bot.send(
            context,
            solutionInfo(student, assignment, problem),
            replyMarkup = Keyboards.solutionMenu(),
          )
      }
    }
  }
}
