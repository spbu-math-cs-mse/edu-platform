package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotState
import com.github.heheteam.commonlib.util.filterByDeadlineAndSort
import com.github.heheteam.commonlib.util.ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.bold
import dev.inmo.tgbotapi.types.message.textsources.italic
import dev.inmo.tgbotapi.types.message.textsources.regular
import dev.inmo.tgbotapi.utils.buildEntities
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char

class CheckDeadlinesState(
  override val context: User,
  private val studentId: StudentId,
  private val course: Course,
) : BotState<Unit, Unit, StudentApi> {
  override suspend fun readUserInput(
    bot: BehaviourContext,
    service: StudentApi,
  ): Result<Unit, FrontendError> = coroutineBinding {
    val problemsWithPersonalDeadlines = service.getActiveProblems(studentId, course.id).bind()
    val messageText =
      buildEntities(" ") {
        problemsWithPersonalDeadlines
          .filterByDeadlineAndSort()
          .sortedBy { it.first.id.long }
          .forEach { (assignment, problems) ->
            +bold(assignment.description) + regular("\n")
            service.calculateRescheduledDeadlines(studentId, problems).forEach { problem ->
              val formattedDeadline = problem.deadline?.format(deadlineFormat)?.let { regular(it) }
              +" • ${problem.number}:   " + (formattedDeadline ?: italic("Без дедлайна")) + "\n"
            }
            +"\n"
          }
      }
    bot.sendMessage(context, messageText)
  }

  private val deadlineFormat =
    LocalDateTime.Format {
      date(
        LocalDate.Format {
          monthName(MonthNames.ENGLISH_ABBREVIATED)
          char(' ')
          dayOfMonth()
        }
      )
      chars(" ")
      time(
        LocalTime.Format {
          hour()
          char(':')
          minute()
        }
      )
    }

  override suspend fun computeNewState(
    service: StudentApi,
    input: Unit,
  ): Result<Pair<State, Unit>, FrontendError> = (MenuState(context, studentId) to input).ok()

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
  ): Result<Unit, FrontendError> = Unit.ok()
}
