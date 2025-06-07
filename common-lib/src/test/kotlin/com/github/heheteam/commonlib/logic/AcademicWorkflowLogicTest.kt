package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ProblemGrade
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionId
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

  private val courseStorage = DatabaseCourseStorage(database)
  private val gradeTable = DatabaseGradeTable(database)
  private val studentStorage = DatabaseStudentStorage(database)
  private val teacherStorage = DatabaseTeacherStorage(database)
  private val submissionDistributor = DatabaseSubmissionDistributor(database)
  private val problemStorage = DatabaseProblemStorage(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)
  private val academicWorkflowLogic = AcademicWorkflowLogic(submissionDistributor, gradeTable)

  private lateinit var courseId: CourseId
  private lateinit var studentId: StudentId
  private lateinit var teacherId: TeacherId
  private lateinit var assignmentId: AssignmentId
  private lateinit var timestamp: Instant

  private val good = SubmissionAssessment(1, TextWithMediaAttachments("comment"))
  private val bad = SubmissionAssessment(0, TextWithMediaAttachments("comment"))

  private fun monotoneTime(): LocalDateTime {
    timestamp += Duration.fromMinutes(1.0)
    return timestamp.toLocalDateTime(TimeZone.UTC)
  }

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
    courseId = courseStorage.createCourse("course 1")
    studentId = studentStorage.createStudent()
    courseStorage.addStudentToCourse(studentId, courseId)
    teacherId = teacherStorage.createTeacher()
    courseStorage.addTeacherToCourse(teacherId, courseId)
    assignmentId = createAssignment(courseId)
    timestamp = Instant.parse("2020-01-01T00:00:00Z")
  }

  private fun createAssignment(courseId: CourseId): AssignmentId =
    assignmentStorage
      .createAssignment(
        courseId,
        "assignment",
        listOf(
          ProblemDescription(1, "p1", "", 1),
          ProblemDescription(3, "p2", "", 1),
          ProblemDescription(2, "p3", "", 1),
        ),
      )
      .value

  private fun inputSubmission(
    academicWorkflowLogic: AcademicWorkflowLogic,
    problemId: ProblemId,
  ): SubmissionId =
    academicWorkflowLogic.inputSubmission(
      SubmissionInputRequest(
        studentId,
        problemId,
        TextWithMediaAttachments(),
        TelegramMessageInfo(RawChatId(0), MessageId(0)),
        monotoneTime(),
      ),
      TeacherId(1L),
    )

  private fun assessSubmissionWithDefaultTeacher(
    academicWorkflowLogic: AcademicWorkflowLogic,
    submissionId: SubmissionId,
    assessment: SubmissionAssessment,
  ) {
    academicWorkflowLogic.assessSubmission(submissionId, teacherId, assessment, monotoneTime())
  }

  @Test
  fun `get gradings for assignment Unsent-Unchecked-Graded`() {
    val submission1Id = inputSubmission(academicWorkflowLogic, ProblemId(1))
    assessSubmissionWithDefaultTeacher(academicWorkflowLogic, submission1Id, good)
    inputSubmission(academicWorkflowLogic, ProblemId(2))
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
  fun `get gradings for assignment two submissions for one problem`() {
    val submission1Id = inputSubmission(academicWorkflowLogic, ProblemId(1))
    assessSubmissionWithDefaultTeacher(academicWorkflowLogic, submission1Id, good)
    val submission2Id = inputSubmission(academicWorkflowLogic, ProblemId(1))
    assessSubmissionWithDefaultTeacher(academicWorkflowLogic, submission2Id, bad)

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
  fun `get gradings for assignment two gradings of one submission`() {
    val submission1Id = inputSubmission(academicWorkflowLogic, ProblemId(1))
    assessSubmissionWithDefaultTeacher(academicWorkflowLogic, submission1Id, good)
    academicWorkflowLogic.assessSubmission(submission1Id, teacherId, bad, monotoneTime())

    val performance =
      gradeTable.getStudentPerformance(studentId, assignmentId).map { it.first.id to it.second }

    assertEquals(ProblemId(1) to 0.toGraded(), performance[0])
  }
}
