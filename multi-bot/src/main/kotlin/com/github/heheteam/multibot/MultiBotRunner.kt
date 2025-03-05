package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.long
import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.run.adminRun
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ObserverBus
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentNotificationService
import com.github.heheteam.commonlib.api.TeacherStatistics
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
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.database.table.DatabaseTelegramSolutionMessagesHandler
import com.github.heheteam.commonlib.decorators.AssignmentStorageDecorator
import com.github.heheteam.commonlib.decorators.CoursesDistributorDecorator
import com.github.heheteam.commonlib.decorators.GradeTableDecorator
import com.github.heheteam.commonlib.decorators.SolutionDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.heheteam.commonlib.mock.MockParentStorage
import com.github.heheteam.commonlib.util.DeveloperOptions
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.heheteam.parentbot.ParentCore
import com.github.heheteam.parentbot.run.parentRun
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.run.studentRun
import com.github.heheteam.teacherbot.CoursesStatisticsResolver
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.heheteam.teacherbot.logic.SolutionGrader
import com.github.heheteam.teacherbot.logic.StudentNewGradeNotifierImpl
import com.github.heheteam.teacherbot.logic.TechnicalMessageUpdaterImpl
import com.github.heheteam.teacherbot.logic.TelegramMessagesJournalUpdater
import com.github.heheteam.teacherbot.logic.UiControllerTelegramSender
import com.github.heheteam.teacherbot.run.teacherRun
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database

class MultiBotRunner : CliktCommand() {
  val studentBotToken: String by option().required().help("student bot token")
  val teacherBotToken: String by option().required().help("teacher bot token")
  val adminBotToken: String by option().required().help("admin bot token")
  val parentBotToken: String by option().required().help("parent bot token")
  val presetStudentId: Long? by option().long()
  val presetTeacherId: Long? by option().long()
  val useRedis: Boolean by option().boolean().default(false)

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
    val teacherStatistics: TeacherStatistics = InMemoryTeacherStatistics()
    val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
      InMemoryScheduledMessagesDistributor()

    val googleSheetsService = GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey)
    val ratingRecorder =
      GoogleSheetsRatingRecorder(
        googleSheetsService,
        coursesDistributor,
        assignmentStorage,
        problemStorage,
        databaseGradeTable,
        solutionDistributor,
      )
    val studentStorage = DatabaseStudentStorage(database)
    //    coursesDistributor.getCourses().forEach { course -> ratingRecorder.updateRating(course.id)
    // }

    val coursesDistributorDecorator =
      CoursesDistributorDecorator(coursesDistributor, ratingRecorder)
    val gradeTable = GradeTableDecorator(databaseGradeTable, ratingRecorder)
    val assignmentStorageDecorator = AssignmentStorageDecorator(assignmentStorage, ratingRecorder)
    val solutionDistributorDecorator =
      SolutionDistributorDecorator(solutionDistributor, ratingRecorder)

    fillWithSamples(
      coursesDistributorDecorator,
      problemStorage,
      assignmentStorage,
      studentStorage,
      teacherStorage,
      database,
    )

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

    val studentCore =
      StudentCore(
        solutionDistributorDecorator,
        coursesDistributorDecorator,
        problemStorage,
        assignmentStorageDecorator,
        gradeTable,
        notificationService,
        botEventBus,
        RandomTeacherResolver(problemStorage, assignmentStorage, coursesDistributor),
      )

    val solutionResolver =
      SolutionResolver(
        solutionDistributor,
        problemStorage,
        assignmentStorageDecorator,
        studentStorage,
      )
    val solutionAssessor =
      SolutionAssessor(
        teacherStatistics,
        solutionDistributor,
        gradeTable,
        problemStorage,
        botEventBus,
      )
    val coursesStatisticsResolver =
      CoursesStatisticsResolver(coursesDistributorDecorator, gradeTable)

    val adminCore =
      AdminCore(
        inMemoryScheduledMessagesDistributor,
        coursesDistributorDecorator,
        studentStorage,
        teacherStorage,
      )

    val parentCore = ParentCore(DatabaseStudentStorage(database), DatabaseGradeTable(database))
    val presetStudent = presetStudentId?.toStudentId()
    val presetTeacher = presetTeacherId?.toTeacherId()
    val developerOptions = DeveloperOptions(presetStudent, presetTeacher)
    val solutionGrader = { bot: BehaviourContext ->
      SolutionGrader(
        gradeTable,
        UiControllerTelegramSender(
          StudentNewGradeNotifierImpl(),
          TelegramMessagesJournalUpdater(
            gradeTable,
            TechnicalMessageUpdaterImpl(bot, DatabaseTelegramSolutionMessagesHandler(database)),
          ),
        ),
      )
    }
    runBlocking {
      launch {
        studentRun(
          studentBotToken,
          studentStorage,
          coursesDistributorDecorator,
          problemStorage,
          studentCore,
          developerOptions,
        )
      }
      launch {
        teacherRun(
          teacherBotToken,
          teacherStorage,
          coursesDistributorDecorator,
          solutionResolver,
          botEventBus,
          solutionAssessor,
          DatabaseTelegramSolutionMessagesHandler(database),
          solutionDistributor,
          solutionGrader,
          developerOptions,
        )
      }
      launch {
        adminRun(
          adminBotToken,
          coursesDistributorDecorator,
          assignmentStorageDecorator,
          problemStorage,
          solutionDistributor,
          adminCore,
        )
      }
      launch { parentRun(parentBotToken, parentStorage, parentCore) }
    }
  }
}
