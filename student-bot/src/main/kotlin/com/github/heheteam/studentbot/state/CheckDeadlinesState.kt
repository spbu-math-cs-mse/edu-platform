package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.studentbot.StudentCore
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardButtons.CallbackDataInlineKeyboardButton
import dev.inmo.tgbotapi.types.chat.User
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

class CheckDeadlinesState(override val context: User, val studentId: StudentId) : BotState

fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnCheckDeadlinesState(core: StudentCore) {
  strictlyOn<CheckDeadlinesState> { state ->
    val course =
      queryCourse(state.context, core.getCourses(), "Выберите курс")
        ?: return@strictlyOn MenuState(state.context, state.studentId)
    val assignments = core.getCourseAssignments(course.id)
    val problemsByAssignments = assignments.associateWith { core.getProblemsFromAssignment(it) }
    val messageText =
      problemsByAssignments.toList().joinToString("\n\n") { (assignment, problems) ->
        assignment.description +
          "\n" +
          problems.joinToString("\n") { problem ->
            println(problem.deadline)
            "  • ${problem.number} ${problem.deadline?.toString()}"
          }
      }
    bot.sendMessage(
      state.context,
      messageText,
      replyMarkup = InlineKeyboardMarkup(CallbackDataInlineKeyboardButton("OK", "ok")),
    )
    waitDataCallbackQueryWithUser(state.context.id).filter { it.data == "ok" }.first()
    MenuState(state.context, state.studentId)
  }
}
