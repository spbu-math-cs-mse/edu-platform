package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Config
import com.github.heheteam.commonlib.Solution
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
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.ScheduledMessagesDistributor
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherStorage
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
import com.github.heheteam.commonlib.util.fillWithSamples
import org.jetbrains.exposed.sql.Database

// we actually should not return that; instead, we must accepts its analogs as inputs to made the
// mocking of telegram services possible
// should be removed ASAP
data class Hack(
  val solutionMessageUpdaterImpl: SolutionMessageUpdaterImpl,
  val menuMessageUpdater: MenuMessageUpdaterImpl,
  val telegramSolutionSender: TelegramSolutionSenderImpl,
)

data class ApiCollection(
  val studentApi: StudentApi,
  val teacherApi: TeacherApi,
  val adminApi: AdminApi,
  val parentApi: ParentApi,
  val hack: Hack,
)

class ApiFabric(
  private val database: Database,
  private val config: Config,
  private val googleSheetsService: GoogleSheetsService,
  private val studentNotificationService: StudentNotificationService,
) {
  @Suppress("LongMethod") // it will always be long-ish, but it is definitely too long (legacy)
  fun createApis(initDatabase: Boolean, useRedis: Boolean): ApiCollection {
    val coursesDistributor = DatabaseCoursesDistributor(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val assignmentStorage: AssignmentStorage = DatabaseAssignmentStorage(database, problemStorage)
    val solutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
    val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
    val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
    val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
      InMemoryScheduledMessagesDistributor()

    val academicWorkflowLogic = AcademicWorkflowLogic(solutionDistributor, databaseGradeTable)
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
        assignmentStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    }

    val parentStorage = MockParentStorage()

    val notificationService = studentNotificationService
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

    botEventBus.subscribeToNewSolutionEvent { solution: Solution ->
      newSolutionTeacherNotifier.notifyNewSolution(solution)
    }
    val teacherApi =
      TeacherApi(
        coursesDistributor,
        academicWorkflowService,
        teacherStorage,
        tgTechnicalMessagesStorage,
      )
    val hack = Hack(solutionMessageService, menuMessageUpdaterService, telegramSolutionSender)
    return ApiCollection(studentApi, teacherApi, adminApi, parentApi, hack)
  }
}
