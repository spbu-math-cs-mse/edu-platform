package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.toGraded
import com.github.heheteam.commonlib.loadConfig
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import korlibs.time.fromMinutes
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database

class AcademicWorkflowLogicTest {
  private val config = loadConfig()

  private val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  private val coursesDistributor = DatabaseCoursesDistributor(database)
  private val gradeTable = DatabaseGradeTable(database)
  private val studentStorage = DatabaseStudentStorage(database)
  private val teacherStorage = DatabaseTeacherStorage(database)
  private val solutionDistributor = DatabaseSolutionDistributor(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database)
  private val problemStorage = DatabaseProblemStorage(database)
  private val academicWorkflowLogic = AcademicWorkflowLogic(solutionDistributor, gradeTable)

  private lateinit var courseId: CourseId
  private lateinit var studentId: StudentId
  private lateinit var teacherId: TeacherId
  private lateinit var assignmentId: AssignmentId
  private lateinit var timestamp: Instant

  val good = SolutionAssessment(1, "comment")
  val bad = SolutionAssessment(0, "comment")

  fun monotoneTime(): LocalDateTime {
    timestamp += Duration.fromMinutes(1.0)
    return timestamp.toLocalDateTime(TimeZone.UTC)
  }

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
    courseId = coursesDistributor.createCourse("course 1")
    studentId = studentStorage.createStudent()
    coursesDistributor.addStudentToCourse(studentId, courseId)
    teacherId = teacherStorage.createTeacher()
    coursesDistributor.addTeacherToCourse(teacherId, courseId)
    assignmentId = createAssignment(courseId)
    timestamp = Instant.parse("2020-01-01T00:00:00Z")
  }

  private fun createAssignment(courseId: CourseId): AssignmentId =
    assignmentStorage.createAssignment(
      courseId,
      "assignment",
      listOf(
        ProblemDescription(1, "p1", "", 1),
        ProblemDescription(3, "p2", "", 1),
        ProblemDescription(2, "p3", "", 1),
      ),
      problemStorage,
    )

  private fun inputSolution(
    academicWorkflowLogic: AcademicWorkflowLogic,
    problemId: ProblemId,
  ): SolutionId =
    academicWorkflowLogic.inputSolution(
      SolutionInputRequest(
        studentId,
        problemId,
        SolutionContent(),
        TelegramMessageInfo(RawChatId(0), MessageId(0)),
        monotoneTime(),
      ),
      TeacherId(1L),
    )

  private fun assessSolutionWithDefaultTeacher(
    academicWorkflowLogic: AcademicWorkflowLogic,
    solutionId: SolutionId,
    assessment: SolutionAssessment,
  ) {
    academicWorkflowLogic.assessSolution(solutionId, teacherId, assessment, monotoneTime())
  }

  @Test
  fun `get gradings for assignment Unsent-Unchecked-Graded`() {
    val solution1Id = inputSolution(academicWorkflowLogic, ProblemId(1))
    assessSolutionWithDefaultTeacher(academicWorkflowLogic, solution1Id, good)
    inputSolution(academicWorkflowLogic, ProblemId(2))
    val performance =
      academicWorkflowLogic.getGradingsForAssignment(assignmentId, studentId).map {
        it.first.id to it.second
      }
    val expected =
      listOf(
        ProblemId(1) to 1.toGraded(),
        ProblemId(2) to ProblemGrade.Unchecked,
        ProblemId(3) to ProblemGrade.Unsent,
      )
    assertEquals(expected, performance)
  }

  @Test
  fun `get gradings for assignment two solutions for one problem`() {
    val solution1Id = inputSolution(academicWorkflowLogic, ProblemId(1))
    assessSolutionWithDefaultTeacher(academicWorkflowLogic, solution1Id, good)
    val solution2Id = inputSolution(academicWorkflowLogic, ProblemId(1))
    assessSolutionWithDefaultTeacher(academicWorkflowLogic, solution2Id, bad)

    val performance =
      academicWorkflowLogic.getGradingsForAssignment(assignmentId, studentId).map {
        it.first.id to it.second
      }

    val expected =
      listOf(
        ProblemId(1) to 0.toGraded(),
        ProblemId(2) to ProblemGrade.Unsent,
        ProblemId(3) to ProblemGrade.Unsent,
      )
    assertEquals(expected, performance)
  }

  @Test
  fun `get gradings for assignment two gradings of one solution`() {
    val solution1Id = inputSolution(academicWorkflowLogic, ProblemId(1))
    assessSolutionWithDefaultTeacher(academicWorkflowLogic, solution1Id, good)
    academicWorkflowLogic.assessSolution(solution1Id, teacherId, bad, monotoneTime())

    val performance =
      gradeTable.getStudentPerformance(studentId, assignmentId).map { it.first.id to it.second }

    assertEquals(ProblemId(1) to 0.toGraded(), performance[0])
  }
}
