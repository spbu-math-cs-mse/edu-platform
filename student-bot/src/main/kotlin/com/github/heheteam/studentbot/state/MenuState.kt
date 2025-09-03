package com.github.heheteam.studentbot.state

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.errors.FrontendError
import com.github.heheteam.commonlib.errors.NumberedError
import com.github.heheteam.commonlib.errors.TokenError
import com.github.heheteam.commonlib.errors.toTelegramError
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.state.BotStateWithHandlersAndStudentId
import com.github.heheteam.commonlib.state.InformationState
import com.github.heheteam.commonlib.state.SuspendableBotAction
import com.github.heheteam.commonlib.util.HandlerResultWithUserInputOrUnhandled
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.commonlib.util.UpdateHandlersController
import com.github.heheteam.commonlib.util.UserInput
import com.github.heheteam.commonlib.util.delete
import com.github.heheteam.commonlib.util.ok
import com.github.heheteam.studentbot.DachshundMenuState
import com.github.heheteam.studentbot.Dialogues
import com.github.heheteam.studentbot.Keyboards.CHALLENGE
import com.github.heheteam.studentbot.Keyboards.CHECK_DEADLINES
import com.github.heheteam.studentbot.Keyboards.CHECK_GRADES
import com.github.heheteam.studentbot.Keyboards.RESCHEDULE_DEADLINES
import com.github.heheteam.studentbot.Keyboards.RETURN_BACK
import com.github.heheteam.studentbot.Keyboards.SEND_SOLUTION
import com.github.heheteam.studentbot.MyCoursesState
import com.github.michaelbull.result.BindingScope
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.runCatching
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.abstracts.AccessibleMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.link

class MenuState(
  override val context: User,
  override val userId: StudentId,
  val courseToken: String? = null,
) : BotStateWithHandlersAndStudentId<State, Unit, StudentApi> {
  private val sentMessages = mutableListOf<AccessibleMessage>()

  override fun defaultState(): State = MenuState(context, userId)

  var selectedCourse: Course? = null

  private suspend fun handleCourseToken(bot: BehaviourContext, service: StudentApi) {
    if (courseToken != null) {
      val registerForCourseWithToken =
        service.registerForCourseWithToken(token = courseToken, userId)
      registerForCourseWithToken.mapBoth(
        success = { course ->
          if (course != null)
            bot.send(context, Dialogues.successfullyRegisteredForCourse(course, courseToken))
        },
        failure = { error ->
          val deepError = error.error
          if (deepError is TokenError)
            bot.send(context, Dialogues.failedToRegisterForCourse(deepError))
          else if (!error.shouldBeIgnored) bot.send(context, error.toMessageText())
        },
      )
    }
  }

  override suspend fun intro(
    bot: BehaviourContext,
    service: StudentApi,
    updateHandlersController: UpdateHandlersController<SuspendableBotAction, State, FrontendError>,
  ): Result<Unit, FrontendError> = coroutineBinding {
    handleCourseToken(bot, service)

    selectedCourse = service.resolveSelectedCourse(userId).bind()

    val stickerMessage = bot.sendSticker(context.id, StudentDialogues.typingSticker)
    val initialMessage =
      bot.send(
        context,
        text = StudentDialogues.menu(selectedCourse),
        replyMarkup = StudentKeyboards.menu(selectedCourse?.id),
      )
    sentMessages.add(stickerMessage)
    sentMessages.add(initialMessage)
    updateHandlersController.addDataCallbackHandler { processKeyboardButtonPresses(it, service) }
  }

  private fun processKeyboardButtonPresses(
    callback: DataCallbackQuery,
    service: StudentApi,
  ): HandlerResultWithUserInputOrUnhandled<Nothing, State, Nothing> {
    val state =
      when (callback.data) {
        StudentKeyboards.MY_COURSES -> noCourseStubState(service).get()
        StudentKeyboards.DACHSHUND_QUEST -> DachshundMenuState(context, userId)
        else -> {
          DachshundMenuState(context, userId).handleKeyboardCallback(callback.data, service).get()
            ?: selectedCourse?.let {
              handleCourseMenuKeyboardCallback(callback.data, it, service).get()
            }
        }
      }
    return if (state != null) {
      UserInput(state)
    } else {
      Unhandled
    }
  }

  private fun noCourseStubState(api: StudentApi): Result<State, NumberedError> = binding {
    val courses = api.getStudentCourses(userId).bind()
    if (courses.isEmpty()) {
      InformationState<StudentApi, StudentId>(
        context,
        userId,
        {
          TextWithMediaAttachments(
              buildEntities {
                +"Пока ты не занимаешься ни на каких наших курсах :(\n"
                +"Записаться на курсы можно " + link("здесь", "https://dabromat.ru/")
              }
            )
            .ok()
        },
        MenuState(context, userId),
      )
    } else {
      MyCoursesState(context, userId, courses)
    }
  }

  private fun handleCourseMenuKeyboardCallback(data: String, course: Course, service: StudentApi) =
    binding {
      when (data) {
        SEND_SOLUTION -> QueryProblemForSubmissionSendingState(context, userId, course.id)
        CHECK_GRADES -> viewGradesNextState(course, service)
        CHECK_DEADLINES -> CheckDeadlinesState(context, userId, course)
        RESCHEDULE_DEADLINES -> RescheduleDeadlinesState(context, userId)
        CHALLENGE -> RequestChallengeState(context, userId, course)
        RETURN_BACK -> MenuState(context, userId)
        else -> null
      }
    }

  private fun BindingScope<FrontendError>.viewGradesNextState(
    course: Course,
    service: StudentApi,
  ): QueryAssignmentForCheckingGradesState =
    QueryAssignmentForCheckingGradesState(
      context,
      userId,
      course.id,
      service.getCourseAssignments(course.id).bind(),
    )

  override suspend fun computeNewState(
    service: StudentApi,
    input: State,
  ): Result<Pair<State, Unit>, FrontendError> {
    return Pair(input, Unit).ok()
  }

  override suspend fun sendResponse(
    bot: BehaviourContext,
    service: StudentApi,
    response: Unit,
    input: State,
  ): Result<Unit, FrontendError> =
    runCatching { sentMessages.forEach { message -> bot.delete(message) } }.toTelegramError()

  override suspend fun outro(bot: BehaviourContext, service: StudentApi) = Unit
}
