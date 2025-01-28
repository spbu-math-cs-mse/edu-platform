package com.github.heheteam.teacherbot.states

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.util.BotState
import com.github.heheteam.teacherbot.Dialogues.noSolutionsToCheck
import com.github.heheteam.teacherbot.TeacherCore
import com.github.michaelbull.result.getOrElse
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

// TODO: Replace TeacherCore with a new, more convenient service
class GettingSolutionState(override val context: User, private val teacherId: TeacherId) :
  BotState<Unit, String?, TeacherCore> {
  private lateinit var solution: Solution
  private lateinit var problem: Problem
  private lateinit var assignment: Assignment
  private lateinit var student: Student

  override suspend fun readUserInput(bot: BehaviourContext, service: TeacherCore) {}

  override suspend fun computeNewState(
    service: TeacherCore,
    input: Unit,
  ): Pair<BotState<*, *, *>, String?> {
    solution =
      service.querySolution(teacherId)
        ?: return Pair(MenuState(context, teacherId), noSolutionsToCheck())

    student =
      service.resolveStudent(solution.studentId).getOrElse { err ->
        return Pair(MenuState(context, teacherId), err.toString())
      }

    problem =
      service.resolveProblem(solution.problemId).getOrElse { err ->
        return Pair(MenuState(context, teacherId), err.toString())
      }

    assignment =
      service.resolveAssignment(problem.assignmentId).getOrElse { err ->
        return Pair(MenuState(context, teacherId), err.toString())
      }

    return Pair(
      CheckingSolutionState(context, teacherId, solution, problem, assignment, student),
      null,
    )
  }

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
