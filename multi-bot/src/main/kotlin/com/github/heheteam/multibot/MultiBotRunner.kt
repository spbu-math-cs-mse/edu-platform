package com.github.heheteam.multibot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.long
import com.github.heheteam.adminbot.run.AdminRunner
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.ObserverBus
import com.github.heheteam.commonlib.api.ParentStorage
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.RatingRecorder
import com.github.heheteam.commonlib.api.RedisBotEventBus
import com.github.heheteam.commonlib.api.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.api.SolutionDistributor
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
import com.github.heheteam.parentbot.run.ParentRunner
import com.github.heheteam.studentbot.run.StudentRunner
import com.github.heheteam.teacherbot.run.TeacherRunner
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class MultiBotRunner : CliktCommand() {
  private val studentBotToken: String by option().required().help("student bot token")
  private val teacherBotToken: String by option().required().help("teacher bot token")
  private val adminBotToken: String by option().required().help("admin bot token")
  private val parentBotToken: String by option().required().help("parent bot token")
  private val presetStudentId: Long? by option().long()
  private val presetTeacherId: Long? by option().long()
  private val useRedis: Boolean by option().boolean().default(false)

  override fun run() {
    val coreModule = injectDependencies()
    startKoin { modules(coreModule) }

    SampleGenerator().fillWithSamples()

    val presetStudent = presetStudentId?.toStudentId()
    val presetTeacher = presetTeacherId?.toTeacherId()
    val developerOptions = DeveloperOptions(presetStudent, presetTeacher)

    runBlocking {
      launch { StudentRunner().run(studentBotToken, developerOptions) }
      launch { TeacherRunner().run(teacherBotToken, developerOptions) }
      launch { AdminRunner().run(adminBotToken) }
      launch { ParentRunner().run(parentBotToken) }
    }
  }

  private fun injectDependencies(): org.koin.core.module.Module {
    val config = loadConfig()
    val database =
      Database.connect(
        config.databaseConfig.url,
        config.databaseConfig.driver,
        config.databaseConfig.login,
        config.databaseConfig.password,
      )
    return module {
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

      single<BotEventBus> {
        if (useRedis) RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)
        else ObserverBus()
      }
    }
  }
}
