package com.github.heheteam.commonlib.studentbot

import com.github.heheteam.commonlib.api.StudentApi
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.RandomTeacherResolver
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CoursesDistributor
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.loadConfig
import com.github.heheteam.commonlib.logic.AcademicWorkflowLogic
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import com.github.heheteam.commonlib.logic.PersonalDeadlinesService
import com.github.heheteam.commonlib.logic.ui.UiController
import com.github.heheteam.commonlib.notifications.BotEventBus
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach

class StudentBotTest {
  private lateinit var coursesDistributor: CoursesDistributor
  private lateinit var solutionDistributor: SolutionDistributor
  private lateinit var studentApi: StudentApi
  private lateinit var courseIds: List<CourseId>
  private lateinit var gradeTable: GradeTable
  private lateinit var studentStorage: StudentStorage
  private lateinit var teacherStorage: TeacherStorage
  private lateinit var problemStorage: ProblemStorage
  private lateinit var assignmentStorage: AssignmentStorage
  private lateinit var academicWorkflowLogic: AcademicWorkflowLogic
  private lateinit var academicWorkflowService: AcademicWorkflowService

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
    coursesDistributor = DatabaseCoursesDistributor(database)
    solutionDistributor = DatabaseSolutionDistributor(database)
    studentStorage = DatabaseStudentStorage(database)
    studentStorage = DatabaseStudentStorage(database)
    teacherStorage = DatabaseTeacherStorage(database)
    problemStorage = DatabaseProblemStorage(database)
    assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)
    gradeTable = DatabaseGradeTable(database)
    academicWorkflowLogic = AcademicWorkflowLogic(solutionDistributor, gradeTable)

    courseIds = (1..4).map { coursesDistributor.createCourse("course $it") }

    val mockBotEventBus = mockk<BotEventBus>(relaxed = true)
    val mockUiController = mockk<UiController>(relaxed = true)
    val mockPersonalDeadlinesService = mockk<PersonalDeadlinesService>(relaxed = true)
    academicWorkflowService =
      AcademicWorkflowService(
        academicWorkflowLogic,
        RandomTeacherResolver(
          problemStorage,
          assignmentStorage,
          coursesDistributor,
          solutionDistributor,
        ),
        mockBotEventBus,
        mockUiController,
      )

    studentApi =
      StudentApi(
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        academicWorkflowService,
        mockPersonalDeadlinesService,
        studentStorage,
      )
  }

  @Test
  fun `new student courses assignment test`() {
    val studentId = studentStorage.createStudent()

    val studentCourses = studentApi.getStudentCourses(studentId)
    assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.long })
  }

  @Test
  fun `new student courses handling test`() {
    val studentId = studentStorage.createStudent()

    studentApi.applyForCourse(studentId, courseIds[0])
    studentApi.applyForCourse(studentId, courseIds[3])

    val studentCourses = studentApi.getStudentCourses(studentId)

    assertEquals(
      listOf(courseIds[0], courseIds[3]),
      studentCourses.map { it.id }.sortedBy { it.long },
    )
    assertEquals(listOf("course 1", "course 4"), studentCourses.map { it.name }.toList())
  }
}
