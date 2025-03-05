package com.github.heheteam.teacherbot.run

import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.table.TelegramSolutionMessagesHandler
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.registerState
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.heheteam.teacherbot.states.ChooseGroupCourseState
import com.github.heheteam.teacherbot.states.DeveloperStartState
import com.github.heheteam.teacherbot.states.ListeningForSolutionsGroupState
import com.github.heheteam.teacherbot.states.MenuState
import com.github.heheteam.teacherbot.states.PresetTeacherState
import com.github.heheteam.teacherbot.states.StartState
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.groupContentMessageOrNull
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.message.content.TextMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Suppress("LongParameterList")
suspend fun teacherRun(
  botToken: String,
  teacherStorage: TeacherStorage,
  coursesDistributor: CoursesDistributor,
  solutionResolver: SolutionResolver,
  botEventBus: BotEventBus,
  solutionAssessor: SolutionAssessor,
  telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  solutionDistributor: SolutionDistributor,
  solutionGraderCreator: (BehaviourContext) -> SolutionGrader,
  developerOptions: DeveloperOptions? = DeveloperOptions(),
) {
  telegramBot(botToken) {
    logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
      println(defaultMessageFormatter(level, tag, message, throwable))
    }
  }

  telegramBotWithBehaviourAndFSMAndStartLongPolling(
      botToken,
      CoroutineScope(Dispatchers.IO),
      onStateHandlingErrorHandler = { state, e ->
        println("Thrown error on $state")
        e.printStackTrace()
        state
      },
    ) {
      println(getMe())
      setMyCommands(
        listOf(
          dev.inmo.tgbotapi.types.BotCommand("start", "start"),
          dev.inmo.tgbotapi.types.BotCommand("end", "endCommand"),
        )
      )
      command("start") { startFsm(it, telegramSolutionMessagesHandler, developerOptions) }
      //      botEventBus.subscribeToNewSolutionEvent { solution: Solution ->
      //        val responsibleTeacherId = solution.responsibleTeacherId
      //        if (responsibleTeacherId != null) {
      //          val responsibleTeacher = teacherStorage.resolveTeacher(responsibleTeacherId)
      //          println("responsible teacher is $responsibleTeacher")
      //          responsibleTeacher.map { responsibleTeacher ->
      //            val chatId = responsibleTeacher.tgId.toChatId()
      //            val solutionMessage = sendSolutionContent(chatId, solution.content)
      //            val solutionGradings = SolutionGradings(solutionId = solution.id)
      //            val content = createTechnicalMessageContent(solutionGradings)
      //            val technicalMessage = reply(solutionMessage, content)
      //            telegramSolutionMessagesHandler.registerPersonalSolutionPublication(
      //              solution.id,
      //              TelegramMessageInfo(technicalMessage.chat.id.chatId,
      // technicalMessage.messageId),
      //            )
      //            editMessageReplyMarkup(
      //              technicalMessage,
      //              replyMarkup = createSolutionGradingKeyboard(solution.id),
      //            )
      //          }
      //        }
      //      }
      val solutionGrader = solutionGraderCreator(this)
      internalCompilerErrorWorkaround(
        solutionResolver,
        botEventBus,
        telegramSolutionMessagesHandler,
        solutionGrader,
      )
      registerState<StartState, TeacherStorage>(teacherStorage)
      registerState<DeveloperStartState, TeacherStorage>(teacherStorage)
      registerMainState(
        teacherStorage,
        solutionDistributor,
        solutionAssessor,
        telegramSolutionMessagesHandler,
      )
      registerState<PresetTeacherState, CoursesDistributor>(coursesDistributor)
      registerState<ChooseGroupCourseState, CoursesDistributor>(coursesDistributor)

      allUpdatesFlow.subscribeSafelyWithoutExceptions(this) { println(it) }
    }
    .second
    .join()
}

private fun DefaultBehaviourContextWithFSM<State>.registerMainState(
  teacherStorage: TeacherStorage,
  solutionDistributor: SolutionDistributor,
  solutionAssessor: SolutionAssessor,
  telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
) {
  strictlyOn<MenuState> { state -> state.handle(this, teacherStorage) }
}

@OptIn(RiskFeature::class)
private suspend fun DefaultBehaviourContextWithFSM<State>.startFsm(
  it: TextMessage,
  telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  developerOptions: DeveloperOptions?,
) {
  val user = it.from
  val groupContent = it.groupContentMessageOrNull()
  if (groupContent != null) {
    sendMessage(groupContent.chat, "greetings!")
    startChain(ChooseGroupCourseState(groupContent.chat))
  } else if (user != null) {
    val startingState = findStartState(developerOptions, user)
    startChain(startingState)
  }
}

private fun DefaultBehaviourContextWithFSM<State>.internalCompilerErrorWorkaround(
  solutionResolver: SolutionResolver,
  botEventBus: BotEventBus,
  telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  solutionGrader: SolutionGrader,
) {
  strictlyOn<ListeningForSolutionsGroupState>(
    registerListeningForSolutionState(
      solutionResolver,
      botEventBus,
      telegramSolutionMessagesHandler,
      solutionGrader,
    )
  )
}

private fun registerListeningForSolutionState(
  solutionResolver: SolutionResolver,
  botEventBus: BotEventBus,
  telegramSolutionMessagesHandler: TelegramSolutionMessagesHandler,
  solutionGrader: SolutionGrader,
): suspend BehaviourContextWithFSM<in State>.(state: ListeningForSolutionsGroupState) -> State? =
  { state ->
    state.execute(
      this,
      solutionResolver,
      telegramSolutionMessagesHandler,
      botEventBus,
      solutionGrader,
    )
  }

private fun findStartState(developerOptions: DeveloperOptions?, user: User) =
  if (developerOptions != null) {
    val presetTeacher = developerOptions.presetTeacherId
    if (presetTeacher != null) {
      PresetTeacherState(user, presetTeacher)
    } else {
      DeveloperStartState(user)
    }
  } else {
    StartState(user)
  }
