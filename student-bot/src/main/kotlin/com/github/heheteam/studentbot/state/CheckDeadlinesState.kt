package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotContext
import com.github.heheteam.commonlib.util.ButtonData
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.buildColumnMenu
import com.github.heheteam.commonlib.util.filterByDeadlineAndSort
import com.github.heheteam.commonlib.util.getCurrentMoscowTime
import com.github.heheteam.commonlib.util.map
import com.github.michaelbull.result.coroutines.coroutineBinding
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
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
  override val userId: StudentId,
  private val course: Course,
) : SimpleStudentState() {
  override fun defaultState(): State = MenuState(context, userId)

  override suspend fun BotContext.run(service: StudentApi) {
    coroutineBinding {
      val problemsWithPersonalDeadlines =
        service
          .getActiveProblems(userId, course.id)
          .bind()
          .filterByDeadlineAndSort(getCurrentMoscowTime())
      val messageText = constructDeadlineMessage(problemsWithPersonalDeadlines, service)
      val okMenu = buildColumnMenu(ButtonData("Хорошо", "ok") { defaultState() }).map(::NewState)
      registerStateMenu(okMenu)
      send(messageText, okMenu.keyboard)
    }
  }

  private fun constructDeadlineMessage(
    problemsWithPersonalDeadlines: List<Pair<Assignment, List<Problem>>>,
    service: StudentApi,
  ): TextSourcesList =
    if (problemsWithPersonalDeadlines.isEmpty()) {
      buildEntities(" ") { +"Нет активных дедлайнов" }
    } else {
      buildEntities(" ") {
        problemsWithPersonalDeadlines
          .sortedBy { it.first.id.long }
          .forEach { (assignment, problems) ->
            +bold(assignment.name) + regular("\n")
            service.calculateRescheduledDeadlines(userId, problems).forEach { problem ->
              val formattedDeadline = problem.deadline?.format(deadlineFormat)?.let { regular(it) }
              +" • ${problem.number}:   " + (formattedDeadline ?: italic("Без дедлайна")) + "\n"
            }
            +"\n"
          }
      }
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
}
