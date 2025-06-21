package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.database.DatabaseAdminStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseCourseTokenStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabasePersonalDeadlineStorage
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseScheduledMessagesDistributor
import com.github.heheteam.commonlib.database.DatabaseSentMessageLogStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.DatabaseTelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.database.FirstTeacherResolver
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.decorators.AssignmentStorageDecorator
import com.github.heheteam.commonlib.decorators.CourseStorageDecorator
import com.github.heheteam.commonlib.decorators.GradeTableDecorator
import com.github.heheteam.commonlib.decorators.SubmissionDistributorDecorator
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsRatingRecorder
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.PersonalDeadlineStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.AcademicWorkflowLogic
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ScheduledMessageDeliveryServiceImpl
import com.github.heheteam.commonlib.logic.ui.MenuMessageUpdaterImpl
import com.github.heheteam.commonlib.logic.ui.NewSubmissionTeacherNotifier
import com.github.heheteam.commonlib.logic.ui.StudentNewGradeNotifierImpl
import com.github.heheteam.commonlib.logic.ui.TelegramMessagesJournalUpdater
import com.github.heheteam.commonlib.logic.ui.UiControllerTelegramSender
import com.github.heheteam.commonlib.mock.MockParentStorage
import com.github.heheteam.commonlib.notifications.BotEventBus
import com.github.heheteam.commonlib.notifications.ObserverBus
import com.github.heheteam.commonlib.telegram.AdminBotTelegramController
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.heheteam.commonlib.util.fillWithSamples
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.logger
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
  private val googleSheetsService: GoogleSheetsService,
  private val studentBotTelegramController: StudentBotTelegramController,
  private val teacherBotTelegramController: TeacherBotTelegramController,
  private val adminBotTelegramController: AdminBotTelegramController,
) {
  @Suppress("LongMethod") // it will always be long-ish, but it is definitely too long (legacy)
  fun createApis(
    initDatabase: Boolean,
    teacherResolverKind: TeacherResolverKind,
    adminIds: List<Long> = listOf(),
  ): ApiCollection {
    val databaseCourseStorage = DatabaseCourseStorage(database)
    val problemStorage: ProblemStorage = DatabaseProblemStorage(database)
    val databaseAssignmentStorage: AssignmentStorage =
      DatabaseAssignmentStorage(database, problemStorage)
    val databaseSubmissionDistributor: SubmissionDistributor =
      DatabaseSubmissionDistributor(database)
    val databaseGradeTable: GradeTable = DatabaseGradeTable(database)
    val teacherStorage: TeacherStorage = DatabaseTeacherStorage(database)
    val sentMessageLogStorage = DatabaseSentMessageLogStorage(database)
    val scheduledMessagesDistributor =
      DatabaseScheduledMessagesDistributor(
        database,
        sentMessageLogStorage,
        studentBotTelegramController,
      )
    val personalDeadlineStorage: PersonalDeadlineStorage = DatabasePersonalDeadlineStorage(database)
    val courseTokenService = DatabaseCourseTokenStorage(database)

    val ratingRecorder =
      GoogleSheetsRatingRecorder(
        googleSheetsService,
        databaseCourseStorage,
        databaseAssignmentStorage,
        problemStorage,
        databaseSubmissionDistributor,
        AcademicWorkflowLogic(databaseSubmissionDistributor, databaseGradeTable),
      )
    val studentStorage = DatabaseStudentStorage(database)

    val courseStorage =
      CourseStorageDecorator(databaseCourseStorage, ratingRecorder, courseTokenService)
    val gradeTable = GradeTableDecorator(databaseGradeTable, ratingRecorder)
    val assignmentStorage = AssignmentStorageDecorator(databaseAssignmentStorage, ratingRecorder)
    val submissionDistributor =
      SubmissionDistributorDecorator(databaseSubmissionDistributor, ratingRecorder)
    val academicWorkflowLogic = AcademicWorkflowLogic(submissionDistributor, gradeTable)
    val adminStorage = DatabaseAdminStorage(database)

    if (initDatabase) {
      fillWithSamples(courseStorage, assignmentStorage, studentStorage, teacherStorage, database)
    }

    adminIds.forEach { adminStorage.addTgIdToWhitelist(it) }

    val parentStorage = MockParentStorage()
    val botEventBus: BotEventBus = ObserverBus()

    botEventBus.subscribeToMovingDeadlineEvents { chatId, newDeadline ->
      studentBotTelegramController.notifyStudentOnDeadlineRescheduling(chatId, newDeadline)
    }

    botEventBus.subscribeToNewDeadlineRequest { studentId, newDeadline ->
      adminStorage
        .getAdmins()
        .onSuccess { admins ->
          admins.forEach { admin ->
            adminBotTelegramController.notifyAdminOnNewMovingDeadlinesRequest(
              admin.tgId,
              studentId,
              newDeadline,
            )
          }
        }
        .onFailure { error ->
          logger.error("Failed to get admins for new deadline request: $error")
        }
    }

    val tgTechnicalMessagesStorage =
      DatabaseTelegramTechnicalMessagesStorage(database, submissionDistributor)
    val menuMessageUpdaterService =
      MenuMessageUpdaterImpl(tgTechnicalMessagesStorage, teacherBotTelegramController)
    val uiController =
      UiControllerTelegramSender(
        StudentNewGradeNotifierImpl(
          studentBotTelegramController,
          problemStorage,
          submissionDistributor,
        ),
        TelegramMessagesJournalUpdater(
          gradeTable,
          submissionDistributor,
          problemStorage,
          assignmentStorage,
          studentStorage,
          teacherStorage,
          tgTechnicalMessagesStorage,
          teacherBotTelegramController,
        ),
        submissionDistributor,
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
            submissionDistributor,
          )
      }

    val newSubmissionTeacherNotifier =
      NewSubmissionTeacherNotifier(
        tgTechnicalMessagesStorage,
        teacherBotTelegramController,
        submissionDistributor,
        problemStorage,
        assignmentStorage,
        studentStorage,
        databaseGradeTable,
        teacherStorage,
        courseStorage,
      )

    val academicWorkflowService =
      AcademicWorkflowService(
        academicWorkflowLogic,
        teacherResolver,
        uiController,
        newSubmissionTeacherNotifier,
      )

    val personalDeadlinesService =
      PersonalDeadlinesService(studentStorage, personalDeadlineStorage, botEventBus)

    val scheduledMessageDeliveryService =
      ScheduledMessageDeliveryServiceImpl(
        scheduledMessagesDistributor,
        courseStorage,
        studentBotTelegramController,
        sentMessageLogStorage,
      )

    val studentApi =
      StudentApi(
        courseStorage,
        problemStorage,
        assignmentStorage,
        academicWorkflowService,
        personalDeadlinesService,
        studentStorage,
        courseTokenService,
        scheduledMessageDeliveryService,
      )

    val adminApi =
      AdminApi(
        scheduledMessagesDistributor,
        courseStorage,
        adminStorage,
        studentStorage,
        teacherStorage,
        assignmentStorage,
        problemStorage,
        submissionDistributor,
        personalDeadlinesService,
        courseTokenService,
      )

    val parentApi = ParentApi(studentStorage, gradeTable, parentStorage)

    botEventBus.subscribeToNewSubmissionEvent { submission: Submission ->
      newSubmissionTeacherNotifier.notifyNewSubmission(submission)
    }

    val teacherApi =
      TeacherApi(courseStorage, academicWorkflowService, teacherStorage, menuMessageUpdaterService)
    return ApiCollection(studentApi, teacherApi, adminApi, parentApi)
  }
}
