package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.util.waitDataCallbackQueryWithUser
import com.github.heheteam.studentbot.Keyboards
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.metaData.back
import com.github.heheteam.studentbot.metaData.buildCoursesSelector
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.deleteMessage
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.reply_markup
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.flow.first

data class SignUpState(override val context: User, val studentId: StudentId) : State

// TODO: rewrite or remove this state

fun DefaultBehaviourContextWithFSM<State>.strictlyOnSignUpState(core: StudentCore) {
  strictlyOn<SignUpState> { state ->
    val studentId = state.studentId
    val courses = core.getCourses()
    val studentCourses = core.getStudentCourses(studentId).toMutableList()
    val coursesToAvailability =
      courses
        .map { course ->
          course to studentCourses.any { studentCourse -> studentCourse.id == course.id }
        }
        .toMutableList()

    val initialMessage =
      bot.send(
        state.context,
        text = "Вот доступные курсы",
        replyMarkup = buildCoursesSelector(coursesToAvailability),
      )

    val signingUpState =
      SigningUpState(state, courses, studentCourses, coursesToAvailability, core, studentId)

    signingUpState.run { signUp(initialMessage) }

    MenuState(state.context, state.studentId)
  }
}

class SigningUpState(
  private val state: SignUpState,
  private val courses: List<Course>,
  private val studentCourses: MutableList<Course>,
  private val coursesToAvailability: MutableList<Pair<Course, Boolean>>,
  private val core: StudentCore,
  private val studentId: StudentId,
) {
  suspend fun BehaviourContext.signUp(initialMessage: ContentMessage<*>) {
    courseIndex(initialMessage)
  }

  private suspend fun BehaviourContext.courseIndex(message: ContentMessage<*>) {
    val callbackData = waitDataCallbackQueryWithUser(state.context.id).first().data

    if (callbackData == Keyboards.APPLY) {
      return applyWithCourses(message)
    }

    val courseId = callbackData.split(" ").last()

    val index =
      when {
        callbackData.contains(Keyboards.COURSE_ID) -> {
          courses.indexOfFirst { it.id == CourseId(courseId.toLong()) }
        }

        else -> {
          deleteMessage(message)
          null
        }
      }

    return courseByIndex(message, index, courseId)
  }

  @OptIn(RiskFeature::class)
  private suspend fun BehaviourContext.courseByIndex(
    message: ContentMessage<*>,
    index: Int?,
    courseId: String,
  ) {
    if (index == null) {
      return
    }

    if (studentCourses.contains(courses[index])) {
      deleteMessage(message)

      val botMessage =
        bot.send(
          state.context,
          text = "Вы уже выбрали этот курс или записаны на него!",
          replyMarkup = back(),
        )

      waitDataCallbackQueryWithUser(state.context.id).first()
      deleteMessage(botMessage)

      val newMessage =
        bot.send(
          state.context,
          text = message.text.toString(),
          replyMarkup = buildCoursesSelector(coursesToAvailability),
        )

      return courseIndex(newMessage)
    }

    studentCourses.add(courses[index])
    coursesToAvailability[
      coursesToAvailability.indexOfFirst { it.first.id == CourseId(courseId.toLong()) },
    ] = courses[index] to true

    bot.editMessageReplyMarkup(
      state.context.id,
      message.messageId,
      replyMarkup = buildCoursesSelector(coursesToAvailability),
    )

    return courseIndex(message)
  }

  @OptIn(RiskFeature::class)
  private suspend fun BehaviourContext.applyWithCourses(message: ContentMessage<*>) {
    when {
      studentCourses.isEmpty() -> {
        deleteMessage(message)

        val botMessage =
          bot.send(state.context, text = "Вы не выбрали ни одного курса!", replyMarkup = back())

        waitDataCallbackQueryWithUser(state.context.id).first()
        deleteMessage(botMessage)

        val newMessage =
          bot.send(
            state.context,
            text = message.text.toString(),
            replyMarkup = message.reply_markup,
          )

        return courseIndex(newMessage)
      }

      else -> {
        studentCourses.forEach { core.addRecord(studentId, it.id) }

        deleteMessage(message)

        val botMessage =
          bot.send(state.context, text = "Вы успешно записались на курсы!", replyMarkup = back())

        waitDataCallbackQueryWithUser(state.context.id).first()
        deleteMessage(botMessage)

        return
      }
    }
  }
}
