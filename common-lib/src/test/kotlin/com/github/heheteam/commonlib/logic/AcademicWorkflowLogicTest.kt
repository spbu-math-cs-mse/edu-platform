package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemGrade
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.toGraded
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.loadConfig
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import korlibs.time.fromMinutes
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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

  private fun inputSolution(problemId: ProblemId): SolutionId =
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

  @Test
  fun `get gradings for assignment Unsent-Unchecked-Graded`() {
    val solution1Id = inputSolution(ProblemId(1))
    academicWorkflowLogic.assessSolution(
      solution1Id,
      teacherId,
      SolutionAssessment(1, "comment"),
      monotoneTime(),
    )
    inputSolution(ProblemId(2))
    val performance =
      academicWorkflowLogic.getGradingsForAssignment(assignmentId, studentId).map {
        it.first.id to it.second
      }

    assertEquals(ProblemId(1) to 1.toGraded(), performance[0])
    assertEquals(ProblemId(2), performance[1].first)
    assertTrue(performance[1].second is ProblemGrade.Unchecked)
    assertEquals(ProblemId(3), performance[2].first)
    assertTrue(performance[2].second is ProblemGrade.Unsent)
    assertTrue(performance.size == 3)
  }

  @Test
  fun `get gradings for assignment two solutions for one problem`() {
    val solution1Id = inputSolution(ProblemId(1))
    academicWorkflowLogic.assessSolution(
      solution1Id,
      teacherId,
      SolutionAssessment(1, "comment"),
      monotoneTime(),
    )
    val solution2Id = inputSolution(ProblemId(1))
    academicWorkflowLogic.assessSolution(
      solution2Id,
      teacherId,
      SolutionAssessment(0, "comment"),
      monotoneTime(),
    )

    val performance =
      academicWorkflowLogic.getGradingsForAssignment(assignmentId, studentId).map {
        it.first.id to it.second
      }

    assertEquals(ProblemId(1) to 0.toGraded(), performance[0])
  }

  @Test
  fun `get gradings for assignment two gradings of one solution`() {
    val solution1Id = inputSolution(ProblemId(1))
    academicWorkflowLogic.assessSolution(
      solution1Id,
      teacherId,
      SolutionAssessment(1, "comment"),
      monotoneTime(),
    )
    academicWorkflowLogic.assessSolution(
      solution1Id,
      teacherId,
      SolutionAssessment(0, "comment"),
      monotoneTime(),
    )

    val performance =
      gradeTable.getStudentPerformance(studentId, assignmentId).map { it.first.id to it.second }

    assertEquals(ProblemId(1) to 0.toGraded(), performance[0])
  }
}
