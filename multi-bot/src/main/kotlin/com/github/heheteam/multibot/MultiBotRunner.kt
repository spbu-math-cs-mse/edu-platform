package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.long
import com.github.heheteam.adminbot.AdminApi
import com.github.heheteam.adminbot.adminRun
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.api.toStudentId
import com.github.heheteam.commonlib.api.toTeacherId
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.DatabaseTelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.decorators.AssignmentStorageDecorator
import com.github.heheteam.commonlib.decorators.CoursesDistributorDecorator
import com.github.heheteam.commonlib.decorators.GradeTableDecorator
import com.github.heheteam.commonlib.decorators.SolutionDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.logic.AcademicWorkflowLogic
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.ui.MenuMessageUpdaterImpl
import com.github.heheteam.commonlib.logic.ui.NewSolutionTeacherNotifier
import com.github.heheteam.commonlib.logic.ui.PrettyTechnicalMessageService
import com.github.heheteam.commonlib.logic.ui.SolutionCourseResolverImpl
import com.github.heheteam.commonlib.logic.ui.SolutionMessageUpdaterImpl
import com.github.heheteam.commonlib.logic.ui.StudentNewGradeNotifierImpl
import com.github.heheteam.commonlib.logic.ui.TelegramMessagesJournalUpdater
import com.github.heheteam.commonlib.logic.ui.TelegramSolutionSenderImpl
import com.github.heheteam.commonlib.logic.ui.UiControllerTelegramSender
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.MockParentStorage
import com.github.heheteam.commonlib.notifications.ObserverBus
import com.github.heheteam.commonlib.notifications.RedisBotEventBus
import com.github.heheteam.commonlib.notifications.StudentNotificationService
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.parentbot.ParentApi
import com.github.heheteam.parentbot.parentRun
import com.github.heheteam.studentbot.StudentApi
import com.github.heheteam.studentbot.studentRun
import com.github.heheteam.teacherbot.StateRegister
import com.github.heheteam.teacherbot.TeacherApi
import com.github.heheteam.teacherbot.TeacherRunner
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

class MultiBotRunner : CliktCommand() {
  private val studentBotToken: String by option().required().help("student bot token")
  private val teacherBotToken: String by option().required().help("teacher bot token")
  private val adminBotToken: String by option().required().help("admin bot token")
  private val parentBotToken: String by option().required().help("parent bot token")
  private val presetStudentId: Long? by option().long()
  private val presetTeacherId: Long? by option().long()
  private val useRedis: Boolean by option().boolean().default(false)
  private val initDatabase: Boolean by option().flag("--noinit", default = true)

  override fun run() {
    val config = loadConfig()
    val database =
      Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
      )

    val coursesDistributor = DatabaseCoursesDistributor(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database)
    val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
    val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
    val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
    val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
      InMemoryScheduledMessagesDistributor()

    val academicWorkflowLogic = AcademicWorkflowLogic(solutionDistributor, databaseGradeTable)
    val googleSheetsService = GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey)
    val ratingRecorder =
      GoogleSheetsRatingRecorder(
        googleSheetsService,
        coursesDistributor,
        assignmentStorage,
        problemStorage,
        solutionDistributor,
        academicWorkflowLogic,
      )
    val studentStorage = DatabaseStudentStorage(database)

    val coursesDistributorDecorator =
      CoursesDistributorDecorator(coursesDistributor, ratingRecorder)
    val gradeTable = GradeTableDecorator(databaseGradeTable, ratingRecorder)
    val assignmentStorageDecorator = AssignmentStorageDecorator(assignmentStorage, ratingRecorder)
    val solutionDistributorDecorator =
      SolutionDistributorDecorator(solutionDistributor, ratingRecorder)

    if (initDatabase) {
      fillWithSamples(
        coursesDistributorDecorator,
        problemStorage,
        assignmentStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    }

    val parentStorage = MockParentStorage()

    val bot =
      telegramBot(studentBotToken) {
        logger = KSLog { level: LogLevel, tag: String?, message: Any, throwable: Throwable? ->
          println(defaultMessageFormatter(level, tag, message, throwable))
        }
      }

    val notificationService = StudentNotificationService(bot)
    val botEventBus =
      if (useRedis) RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)
      else ObserverBus()

    botEventBus.subscribeToGradeEvents { studentId, chatId, messageId, assessment, problem ->
      notificationService.notifyStudentAboutGrade(studentId, chatId, messageId, assessment, problem)
    }

    val tgTechnicalMessagesStorage =
      DatabaseTelegramTechnicalMessagesStorage(database, solutionDistributor)
    val prettyTechnicalMessageService =
      PrettyTechnicalMessageService(
        solutionDistributorDecorator,
        problemStorage,
        assignmentStorage,
        studentStorage,
        databaseGradeTable,
        teacherStorage,
      )
    val solutionMessageService =
      SolutionMessageUpdaterImpl(tgTechnicalMessagesStorage, prettyTechnicalMessageService)
    val menuMessageUpdaterService = MenuMessageUpdaterImpl(tgTechnicalMessagesStorage)
    val uiController =
      UiControllerTelegramSender(
        StudentNewGradeNotifierImpl(botEventBus, problemStorage, solutionDistributor),
        TelegramMessagesJournalUpdater(gradeTable, solutionMessageService),
        menuMessageUpdaterService,
        solutionDistributor,
      )
    val teacherResolver: ResponsibleTeacherResolver =
      //      FirstTeacherResolver(problemStorage, assignmentStorage, coursesDistributor)
      RandomTeacherResolver(
        problemStorage,
        assignmentStorage,
        coursesDistributor,
        solutionDistributor,
      )
    val academicWorkflowService =
      AcademicWorkflowService(academicWorkflowLogic, teacherResolver, botEventBus, uiController)
    val studentApi =
      StudentApi(
        coursesDistributorDecorator,
        problemStorage,
        assignmentStorageDecorator,
        academicWorkflowService,
        studentStorage,
      )

    val adminApi =
      AdminApi(
        inMemoryScheduledMessagesDistributor,
        coursesDistributorDecorator,
        studentStorage,
        teacherStorage,
        assignmentStorage,
        problemStorage,
        solutionDistributor,
      )

    val parentApi =
      ParentApi(DatabaseStudentStorage(database), DatabaseGradeTable(database), parentStorage)
    val presetStudent = presetStudentId?.toStudentId()
    val presetTeacher = presetTeacherId?.toTeacherId()
    val developerOptions = DeveloperOptions(presetStudent, presetTeacher)

    val telegramSolutionSender =
      TelegramSolutionSenderImpl(teacherStorage, prettyTechnicalMessageService, coursesDistributor)
    val solutionCourseResolver =
      SolutionCourseResolverImpl(solutionDistributor, problemStorage, assignmentStorageDecorator)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(
        telegramSolutionSender,
        tgTechnicalMessagesStorage,
        solutionCourseResolver,
        menuMessageUpdaterService,
      )

    val teacherApi =
      TeacherApi(
        coursesDistributor,
        academicWorkflowService,
        teacherStorage,
        tgTechnicalMessagesStorage,
      )
    runBlocking {
      launch { studentRun(studentBotToken, studentApi, developerOptions) }
      launch {
        val stateRegister = StateRegister(teacherApi)
        val teacherRunner =
          TeacherRunner(teacherBotToken, botEventBus, stateRegister, developerOptions)
        teacherRunner.execute(
          newSolutionTeacherNotifier,
          listOf(solutionMessageService, menuMessageUpdaterService, telegramSolutionSender),
        )
      }
      launch { adminRun(adminBotToken, adminApi) }
      launch { parentRun(parentBotToken, parentApi) }
    }
  }
}
