package com.github.heheteam.commonlib.studentbot

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.config.loadConfig
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.errors.ErrorManagementService
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.logic.AcademicWorkflowLogic
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.CourseTokenService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ScheduledMessageService
import com.github.heheteam.commonlib.logic.StudentViewService
import com.github.heheteam.commonlib.logic.ui.NewSubmissionTeacherNotifier
import com.github.heheteam.commonlib.logic.ui.UiController
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach

class StudentBotTest {
  private lateinit var courseStorage: CourseStorage
  private lateinit var submissionDistributor: SubmissionDistributor
  private lateinit var studentApi: StudentApi
  private lateinit var courseIds: List<CourseId>
  private lateinit var gradeTable: GradeTable
  private lateinit var studentStorage: StudentStorage
  private lateinit var teacherStorage: TeacherStorage
  private lateinit var problemStorage: ProblemStorage
  private lateinit var assignmentStorage: AssignmentStorage
  private lateinit var academicWorkflowLogic: AcademicWorkflowLogic
  private lateinit var academicWorkflowService: AcademicWorkflowService
  private lateinit var scheduledMessageService: ScheduledMessageService
  private val config = loadConfig()

  private val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  @BeforeEach
  fun setup() {
    reset(database)
    courseStorage = DatabaseCourseStorage(database)
    initDatabaseStorages()
    academicWorkflowLogic = AcademicWorkflowLogic(submissionDistributor, gradeTable)
    scheduledMessageService = mockk<ScheduledMessageService>(relaxed = true)
    courseIds = (1..4).map { courseStorage.createCourse("course $it").value }
    val mockUiController = mockk<UiController>(relaxed = true)
    val mockPersonalDeadlinesService = mockk<PersonalDeadlinesService>(relaxed = true)
    val mockCourseTokensService = mockk<CourseTokenService>(relaxed = true)
    val teacherNotifier = mockk<NewSubmissionTeacherNotifier>()
    academicWorkflowService =
      AcademicWorkflowService(
        academicWorkflowLogic,
        RandomTeacherResolver(
          problemStorage,
          assignmentStorage,
          courseStorage,
          submissionDistributor,
        ),
        mockUiController,
        teacherNotifier,
      )
    studentApi =
      StudentApi(
        academicWorkflowService,
        mockPersonalDeadlinesService,
        scheduledMessageService,
        StudentViewService(courseStorage, problemStorage, assignmentStorage),
        studentStorage,
        mockCourseTokensService,
        ErrorManagementService(),
      )
  }

  private fun initDatabaseStorages() {
    submissionDistributor = DatabaseSubmissionDistributor(database)
    studentStorage = DatabaseStudentStorage(database)
    studentStorage = DatabaseStudentStorage(database)
    teacherStorage = DatabaseTeacherStorage(database)
    problemStorage = DatabaseProblemStorage(database)
    assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)
    gradeTable = DatabaseGradeTable(database)
  }

  @Test
  fun `new student courses assignment test`() {
    val studentId = studentStorage.createStudent().value

    val studentCourses = studentApi.getStudentCourses(studentId).value
    assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.long })
  }
}
