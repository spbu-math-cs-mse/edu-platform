package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.sendSolutionContent
import com.github.heheteam.commonlib.util.waitTextMessageWithUser
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.utils.contentMessageOrNull
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.textedContentOrNull
import dev.inmo.tgbotapi.types.chat.Chat
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ListeningForSolutionsGroupState(override val context: Chat, val courseId: CourseId) : State {
  @OptIn(PreviewFeature::class, RiskFeature::class)
  suspend fun execute(
    bot: BehaviourContext,
    solutionResolver: SolutionResolver,
    solutionDistributor: SolutionDistributor,
    solutionAssessor: SolutionAssessor,
    bus: BotEventBus,
  ): State {
    with(bot) {
      bus.subscribeToNewSolutionEvent { solution: Solution ->
        println("received event about $solution")
        val belongsToChat =
          binding {
              val problem = solutionResolver.resolveProblem(solution.problemId).bind()
              val assignment = solutionResolver.resolveAssignment(problem.assignmentId).bind()
              assignment.courseId == courseId
            }
            .get() ?: false
        if (belongsToChat) {
          sendSolutionIntoGroup(solution)
        }
      }
      waitTextMessageWithUser(context.id.toChatId())
        .map { commonMessage ->
          println(commonMessage)
          val comment = commonMessage.text.orEmpty()
          val error =
            binding {
                val reply = commonMessage.replyTo.toResultOr { "not a reply" }.bind()
                val regex = Regex(".*\\[(.*)]", option = RegexOption.DOT_MATCHES_ALL)
                val text =
                  reply
                    .contentMessageOrNull()
                    ?.content
                    ?.textedContentOrNull()
                    ?.text
                    .toResultOr { "message not a text" }
                    .bind()
                val grade = if (comment.contains("[+]")) 1 else 0
                val match = regex.matchEntire(text).toResultOr { "Not a submission message" }.bind()
                val submissionInfo =
                  runCatching {
                      match.groups[1]?.value!!.let { Json.decodeFromString<SubmissionInfo>(it) }
                    }
                    .mapError { "Bad regex or failed submission format" }
                    .bind()
                val solution =
                  solutionDistributor
                    .resolveSolution(submissionInfo.solutionId)
                    .mapError { "failed to resolve solution" }
                    .bind()
                val assessment = SolutionAssessment(grade, commonMessage.text.orEmpty())
                solutionAssessor.assessSolution(solution, TeacherId(1L), assessment)
              }
              .getError()
          if (error != null) {
            sendMessage(context, error)
          }
        }
        .first()
      return this@ListeningForSolutionsGroupState
    }
  }

  private suspend fun BehaviourContext.sendSolutionIntoGroup(solution: Solution) {
    val submissionSignature = Json.encodeToString(SubmissionInfo(solutionId = solution.id))
    sendSolutionContent(
      context.id.toChatId(),
      solution.content.copy(text = "${solution.content.text}\n[$submissionSignature]"),
    )
  }

  @Serializable private data class SubmissionInfo(val solutionId: SolutionId)
}
