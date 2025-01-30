package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.teacherbot.Dialogues.noSolutionsToCheck
import com.github.heheteam.teacherbot.TeacherCore
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.chat.User
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels

private fun <V, E> Result<V, E>.toStrErr(): Result<V, String> = this.mapError { it.toString() }

// TODO: Replace TeacherCore with a new, more convenient service
class GettingSolutionState(override val context: User, private val teacherId: TeacherId) :
  BotState<Unit, String?, TeacherCore> {

  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherCore) = Unit

  override suspend fun computeNewState(
    service: TeacherCore,
    input: Unit,
  ): Pair<BotState<*, *, *>, String?> =
    binding {
        val solution = service.querySolution(teacherId).toResultOr { noSolutionsToCheck() }.bind()
        val student = service.resolveStudent(solution.studentId).toStrErr().bind()
        val problem = service.resolveProblem(solution.problemId).toStrErr().bind()
        val assignment = service.resolveAssignment(problem.assignmentId).toStrErr().bind()
        Pair(
          CheckingSolutionState(context, teacherId, solution, problem, assignment, student),
          null as String?,
        )
      }
      .getOrElse { Pair(MenuState(context, teacherId), it) }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: TeacherCore,
    response: String?,
  ) {
    if (response != null) {
      bot.send(context, response)
      return
    }
  }
}

internal object SolutionProvider {
  private var fileIndex = 0

  fun getSolutionWithURL(fileURL: String): Pair<MultipartFile, File> {
    val url: URL = URI(fileURL).toURL()
    val outputFileName = "solution${fileIndex++}.${fileURL.substringAfterLast(".")}"
    val file = File(outputFileName)

    url.openStream().use {
      Channels.newChannel(it).use { rbc ->
        FileOutputStream(outputFileName).use { fos ->
          fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
        }
      }
    }

    return file.asMultipartFile() to file
  }
}
