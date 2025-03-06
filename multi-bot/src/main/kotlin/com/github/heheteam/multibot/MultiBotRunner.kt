package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.long
import com.github.heheteam.adminbot.AdminCore
import com.github.heheteam.adminbot.run.AdminRunner
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ObserverBus
import com.github.heheteam.commonlib.api.ParentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.StudentNotificationService
import com.github.heheteam.commonlib.api.StudentStorage
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
import com.github.heheteam.commonlib.util.SampleGenerator
import com.github.heheteam.parentbot.ParentCore
import com.github.heheteam.parentbot.run.ParentRunner
import com.github.heheteam.studentbot.StudentCore
import com.github.heheteam.studentbot.run.StudentRunner
import com.github.heheteam.teacherbot.CoursesStatisticsResolver
import com.github.heheteam.teacherbot.SolutionAssessor
import com.github.heheteam.teacherbot.SolutionResolver
import com.github.heheteam.teacherbot.run.TeacherRunner
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.LogLevel
import dev.inmo.kslog.common.defaultMessageFormatter
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

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

    val appModule = module {
      single<Database> { database }
      val coursesDistributor = DatabaseCoursesDistributor(database)
      val gradeTable = DatabaseGradeTable(database)
      val assignmentStorage = DatabaseAssignmentStorage(database)
      val solutionDistributor = DatabaseSolutionDistributor(database)
      single<ProblemStorage> { DatabaseProblemStorage(database) }
      single<TeacherStorage> { DatabaseTeacherStorage(database) }
      single<TeacherStatistics> { InMemoryTeacherStatistics() }
      single<ScheduledMessagesDistributor> { InMemoryScheduledMessagesDistributor() }
      single<StudentStorage> { DatabaseStudentStorage(database) }
      single<ParentStorage> { MockParentStorage() }

      val googleSheetsService = GoogleSheetsService(config.googleSheetsConfig.serviceAccountKey)
      single<RatingRecorder> {
        GoogleSheetsRatingRecorder(
          googleSheetsService,
          coursesDistributor,
          assignmentStorage,
          problemStorage = get(),
          gradeTable,
          solutionDistributor,
        )
      }

      single<CoursesDistributor> {
        CoursesDistributorDecorator(coursesDistributor, ratingRecorder = get())
      }
      single<GradeTable> { GradeTableDecorator(gradeTable, ratingRecorder = get()) }
      single<AssignmentStorage> {
        AssignmentStorageDecorator(assignmentStorage, ratingRecorder = get())
      }
      single<SolutionDistributor> {
        SolutionDistributorDecorator(solutionDistributor, ratingRecorder = get())
      }
    }

    startKoin { modules(appModule) }

    SampleGenerator().fillWithSamples()

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
    val studentCore = StudentCore(notificationService, botEventBus)

    val solutionResolver = SolutionResolver()
    val solutionAssessor = SolutionAssessor(botEventBus)
    val coursesStatisticsResolver = CoursesStatisticsResolver()

    val adminCore = AdminCore()

    val parentCore = ParentCore(DatabaseStudentStorage(database), DatabaseGradeTable(database))
    val presetStudent = presetStudentId?.toStudentId()
    val presetTeacher = presetTeacherId?.toTeacherId()
    val developerOptions = DeveloperOptions(presetStudent, presetTeacher)
    runBlocking {
      launch { StudentRunner().run(studentBotToken, studentCore, developerOptions) }
      launch {
        TeacherRunner()
          .run(
            teacherBotToken,
            coursesStatisticsResolver,
            solutionResolver,
            botEventBus,
            solutionAssessor,
            developerOptions,
          )
      }
      launch { AdminRunner().run(adminBotToken, adminCore) }
      launch { ParentRunner().run(parentBotToken, parentCore) }
    }
  }
}
