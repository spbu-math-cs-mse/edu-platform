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
import com.github.heheteam.commonlib.database.FirstTeacherResolver
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
import com.github.heheteam.commonlib.logic.ui.StudentNewGradeNotifierImpl
import com.github.heheteam.commonlib.logic.ui.TelegramMessagesJournalUpdater
import com.github.heheteam.commonlib.logic.ui.UiControllerTelegramSender
import com.github.heheteam.commonlib.mock.InMemoryScheduledMessagesDistributor
import com.github.heheteam.commonlib.mock.MockParentStorage
import com.github.heheteam.commonlib.notifications.ObserverBus
import com.github.heheteam.commonlib.notifications.RedisBotEventBus
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
) {
  @Suppress("LongMethod") // it will always be long-ish, but it is definitely too long (legacy)
  fun createApis(
    initDatabase: Boolean,
    useRedis: Boolean,
    teacherResolverKind: TeacherResolverKind,
  ): ApiCollection {
    val databaseCoursesDistributor = DatabaseCoursesDistributor(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val databaseAssignmentStorage: AssignmentStorage =
      DatabaseAssignmentStorage(database, problemStorage)
    val databaseSolutionDistributor: SolutionDistributor = DatabaseSolutionDistributor(database)
    val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
    val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
    val inMemoryScheduledMessagesDistributor: ScheduledMessagesDistributor =
      InMemoryScheduledMessagesDistributor()

    val ratingRecorder =
      GoogleSheetsRatingRecorder(
        googleSheetsService,
        databaseCoursesDistributor,
        databaseAssignmentStorage,
        problemStorage,
        databaseSolutionDistributor,
        AcademicWorkflowLogic(databaseSolutionDistributor, databaseGradeTable),
      )
    val studentStorage = DatabaseStudentStorage(database)

    val coursesDistributor = CoursesDistributorDecorator(databaseCoursesDistributor, ratingRecorder)
    val gradeTable = GradeTableDecorator(databaseGradeTable, ratingRecorder)
    val assignmentStorage = AssignmentStorageDecorator(databaseAssignmentStorage, ratingRecorder)
    val solutionDistributor =
      SolutionDistributorDecorator(databaseSolutionDistributor, ratingRecorder)
    val academicWorkflowLogic = AcademicWorkflowLogic(solutionDistributor, gradeTable)
    if (initDatabase) {
      fillWithSamples(
        coursesDistributor,
        assignmentStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    }

    val parentStorage = MockParentStorage()

    val botEventBus =
      if (useRedis) RedisBotEventBus(config.redisConfig.host, config.redisConfig.port)
      else ObserverBus()

    botEventBus.subscribeToGradeEvents { studentId, chatId, messageId, assessment, problem ->
      studentBotTelegramController.notifyStudentOnNewAssessment(
        chatId,
        messageId,
        studentId,
        problem,
        assessment,
      )
    }

    val tgTechnicalMessagesStorage =
      DatabaseTelegramTechnicalMessagesStorage(database, solutionDistributor)
    val menuMessageUpdaterService =
      MenuMessageUpdaterImpl(tgTechnicalMessagesStorage, teacherBotTelegramController)
    val uiController =
      UiControllerTelegramSender(
        StudentNewGradeNotifierImpl(botEventBus, problemStorage, solutionDistributor),
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
          FirstTeacherResolver(problemStorage, assignmentStorage, coursesDistributor)
        TeacherResolverKind.RANDOM ->
          RandomTeacherResolver(
            problemStorage,
            assignmentStorage,
            coursesDistributor,
            solutionDistributor,
          )
      }
    val academicWorkflowService =
      AcademicWorkflowService(academicWorkflowLogic, teacherResolver, botEventBus, uiController)
    val studentApi =
      StudentApi(
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        academicWorkflowService,
        studentStorage,
      )

    val adminApi =
      AdminApi(
        inMemoryScheduledMessagesDistributor,
        coursesDistributor,
        studentStorage,
        teacherStorage,
        assignmentStorage,
        problemStorage,
        solutionDistributor,
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
        coursesDistributor,
      )

    botEventBus.subscribeToNewSolutionEvent { solution: Solution ->
      newSolutionTeacherNotifier.notifyNewSolution(solution)
    }
    val teacherApi =
      TeacherApi(
        coursesDistributor,
        academicWorkflowService,
        teacherStorage,
        menuMessageUpdaterService,
      )
    return ApiCollection(studentApi, teacherApi, adminApi, parentApi)
  }
}
