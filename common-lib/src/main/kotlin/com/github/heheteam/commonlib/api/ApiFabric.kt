package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Config
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.database.DatabaseAdminStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseCourseTokenStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabasePersonalDeadlineStorage
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.DatabaseTelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.database.FirstTeacherResolver
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.decorators.AssignmentStorageDecorator
import com.github.heheteam.commonlib.decorators.CourseStorageDecorator
import com.github.heheteam.commonlib.decorators.GradeTableDecorator
import com.github.heheteam.commonlib.decorators.SolutionDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.PersonalDeadlineStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.AcademicWorkflowLogic
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ui.MenuMessageUpdaterImpl
import com.github.heheteam.commonlib.logic.ui.NewSolutionTeacherNotifier
import com.github.heheteam.commonlib.logic.ui.StudentNewGradeNotifierImpl
import com.github.heheteam.commonlib.logic.ui.TelegramMessagesJournalUpdater
import com.github.heheteam.commonlib.logic.ui.UiControllerTelegramSender
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.MockParentStorage
import com.github.heheteam.commonlib.notifications.BotEventBus
import com.github.heheteam.commonlib.notifications.ObserverBus
import com.github.heheteam.commonlib.notifications.RedisBotEventBus
import com.github.heheteam.commonlib.telegram.AdminBotTelegramController
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.heheteam.commonlib.util.fillWithSamples
import org.jetbrains.exposed.sql.Database

data class ApiCollection(
  val studentApi: StudentApi,
  val teacherApi: TeacherApi,
  val adminApi: AdminApi,
  val parentApi: ParentApi,
)

enum class TeacherResolverKind {
  FIRST,
  RANDOM,
}

class ApiFabric(
  private val database: Database,
  private val config: Config,
  private val googleSheetsService: GoogleSheetsService,
  private val studentBotTelegramController: StudentBotTelegramController,
  private val teacherBotTelegramController: TeacherBotTelegramController,
  private val adminBotTelegramController: AdminBotTelegramController,
) {
  @Suppress("LongMethod") // it will always be long-ish, but it is definitely too long (legacy)
  fun createApis(
    initDatabase: Boolean,
    useRedis: Boolean,
    teacherResolverKind: TeacherResolverKind,
  ): ApiCollection {
    val databaseCourseStorage = DatabaseCourseStorage(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val databaseAssignmentStorage: AssignmentStorage =
      DatabaseAssignmentStorage(database, problemStorage)
    val databaseSolutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
    val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
    val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
    val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
      InMemoryScheduledMessagesDistributor()
    val personalDeadlineStorage: PersonalDeadlineStorage = DatabasePersonalDeadlineStorage(database)
    val courseTokenService = DatabaseCourseTokenStorage(database)

    val ratingRecorder =
      GoogleSheetsRatingRecorder(
        googleSheetsService,
        databaseCourseStorage,
        databaseAssignmentStorage,
        problemStorage,
        databaseSolutionDistributor,
        AcademicWorkflowLogic(databaseSolutionDistributor, databaseGradeTable),
      )
    val studentStorage = DatabaseStudentStorage(database)

    val courseStorage =
      CourseStorageDecorator(databaseCourseStorage, ratingRecorder, courseTokenService)
    val gradeTable = GradeTableDecorator(databaseGradeTable, ratingRecorder)
    val assignmentStorage = AssignmentStorageDecorator(databaseAssignmentStorage, ratingRecorder)
    val solutionDistributor =
      SolutionDistributorDecorator(databaseSolutionDistributor, ratingRecorder)
    val academicWorkflowLogic = AcademicWorkflowLogic(solutionDistributor, gradeTable)
    val adminStorage = DatabaseAdminStorage(database)
    if (initDatabase) {
      fillWithSamples(
        courseStorage,
        assignmentStorage,
        adminStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    }

    val parentStorage = MockParentStorage()
    val botEventBus: BotEventBus =
      if (useRedis) RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)
      else ObserverBus()

    botEventBus.subscribeToMovingDeadlineEvents { chatId, newDeadline ->
      studentBotTelegramController.notifyStudentOnDeadlineRescheduling(chatId, newDeadline)
    }

    botEventBus.subscribeToNewDeadlineRequest { studentId, newDeadline ->
      adminStorage.getAdmins().forEach { admin ->
        adminBotTelegramController.notifyAdminOnNewMovingDeadlinesRequest(
          admin.tgId,
          studentId,
          newDeadline,
        )
      }
    }

    val tgTechnicalMessagesStorage =
      DatabaseTelegramTechnicalMessagesStorage(database, solutionDistributor)
    val menuMessageUpdaterService =
      MenuMessageUpdaterImpl(tgTechnicalMessagesStorage, teacherBotTelegramController)
    val uiController =
      UiControllerTelegramSender(
        StudentNewGradeNotifierImpl(
          studentBotTelegramController,
          problemStorage,
          solutionDistributor,
        ),
        TelegramMessagesJournalUpdater(
          gradeTable,
          solutionDistributor,
          problemStorage,
          assignmentStorage,
          studentStorage,
          teacherStorage,
          tgTechnicalMessagesStorage,
          teacherBotTelegramController,
        ),
        solutionDistributor,
        teacherBotTelegramController,
        tgTechnicalMessagesStorage,
      )
    val teacherResolver: ResponsibleTeacherResolver =
      when (teacherResolverKind) {
        TeacherResolverKind.FIRST ->
          FirstTeacherResolver(problemStorage, assignmentStorage, courseStorage)

        TeacherResolverKind.RANDOM ->
          RandomTeacherResolver(
            problemStorage,
            assignmentStorage,
            courseStorage,
            solutionDistributor,
          )
      }
    val academicWorkflowService =
      AcademicWorkflowService(academicWorkflowLogic, teacherResolver, botEventBus, uiController)

    val personalDeadlinesService =
      PersonalDeadlinesService(studentStorage, personalDeadlineStorage, botEventBus)

    val studentApi =
      StudentApi(
        courseStorage,
        problemStorage,
        assignmentStorage,
        academicWorkflowService,
        personalDeadlinesService,
        studentStorage,
        courseTokenService,
      )

    val adminApi =
      AdminApi(
        inMemoryScheduledMessagesDistributor,
        courseStorage,
        adminStorage,
        studentStorage,
        teacherStorage,
        assignmentStorage,
        problemStorage,
        solutionDistributor,
        personalDeadlinesService,
        courseTokenService,
      )

    val parentApi = ParentApi(studentStorage, gradeTable, parentStorage)
    val newSolutionTeacherNotifier =
      NewSolutionTeacherNotifier(
        tgTechnicalMessagesStorage,
        teacherBotTelegramController,
        solutionDistributor,
        problemStorage,
        assignmentStorage,
        studentStorage,
        databaseGradeTable,
        teacherStorage,
        courseStorage,
      )

    botEventBus.subscribeToNewSolutionEvent { solution: Solution ->
      newSolutionTeacherNotifier.notifyNewSolution(solution)
    }

    val teacherApi =
      TeacherApi(courseStorage, academicWorkflowService, teacherStorage, menuMessageUpdaterService)
    return ApiCollection(studentApi, teacherApi, adminApi, parentApi)
  }
}
