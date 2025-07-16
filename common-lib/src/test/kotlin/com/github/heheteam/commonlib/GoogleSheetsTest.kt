package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.config.loadConfig
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseRepository
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsServiceImpl
import com.github.heheteam.commonlib.interfaces.ProblemId
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.jetbrains.exposed.sql.Database

@Ignore
class GoogleSheetsTest {
  private val config = loadConfig()

  private val database =
    Database.connect(
      config.databaseConfig.url,
      config.databaseConfig.driver,
      config.databaseConfig.login,
      config.databaseConfig.password,
    )

  private val courseStorage = DatabaseCourseStorage(DatabaseCourseRepository())
  private val gradeTable = DatabaseGradeTable(database)
  private val studentStorage = DatabaseStudentStorage(database)
  private val teacherStorage = DatabaseTeacherStorage(database)
  private val submissionDistributor = DatabaseSubmissionDistributor(database)
  private val problemStorage = DatabaseProblemStorage(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)
  private val googleSheetsService =
    GoogleSheetsServiceImpl(config.googleSheetsConfig.serviceAccountKeyPath)

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
  }

  //  @Ignore
  @Test
  fun `update rating works`() {
    val course1Id = courseStorage.createCourse("course 1").value
    val course2Id = courseStorage.createCourse("course 2").value
    val student1Id = studentStorage.createStudent(grade = null, from = null).value
    val student2Id = studentStorage.createStudent(grade = null, from = null).value
    val student3Id = studentStorage.createStudent(grade = null, from = null).value
    courseStorage.addStudentToCourse(student1Id, course1Id)
    courseStorage.addStudentToCourse(student1Id, course2Id)
    courseStorage.addStudentToCourse(student2Id, course1Id)
    courseStorage.addStudentToCourse(student2Id, course2Id)
    courseStorage.addStudentToCourse(student3Id, course1Id)

    val teacher1Id = teacherStorage.createTeacher()
    courseStorage.addTeacherToCourse(teacher1Id, course1Id)
    courseStorage.addTeacherToCourse(teacher1Id, course2Id)

    assignmentStorage.createAssignment(
      course1Id,
      "assignment 1",
      listOf(ProblemDescription(1, "p1"), ProblemDescription(2, "p2"), ProblemDescription(3, "p3")),
    )
    assignmentStorage.createAssignment(
      course1Id,
      "assignment 2",
      listOf(ProblemDescription(1, "p1"), ProblemDescription(2, "p2"), ProblemDescription(3, "p3")),
    )
    assignmentStorage.createAssignment(
      course1Id,
      "assignment 3",
      listOf(ProblemDescription(1, "p1"), ProblemDescription(2, "p2"), ProblemDescription(3, "p3")),
    )
    assignmentStorage.createAssignment(
      course2Id,
      "assignment 1",
      listOf(
        ProblemDescription(1, "p1"),
        ProblemDescription(2, "p2"),
        ProblemDescription(3, "p3"),
        ProblemDescription(4, "p4"),
      ),
    )

    for (problemId in 1..11) {
      submissionDistributor.inputSubmission(
        student1Id,
        RawChatId(0),
        MessageId(0),
        TextWithMediaAttachments(),
        ProblemId(problemId.toLong()),
        LocalDateTime.now(),
        teacher1Id,
      )
    }
    for (problemId in 1..6) {
      submissionDistributor.inputSubmission(
        student2Id,
        RawChatId(0),
        MessageId(0),
        TextWithMediaAttachments(),
        ProblemId(problemId.toLong() * 2),
        LocalDateTime.now(),
        teacher1Id,
      )
    }

    for (submissionId in 1..17) {
      val submission = submissionDistributor.querySubmission(teacher1Id).get()
      assertNotNull(submission)
      gradeTable.recordSubmissionAssessment(
        submission.id,
        teacher1Id,
        SubmissionAssessment(submissionId % 2, TextWithMediaAttachments.fromString("comment")),
      )
    }

    val spreadsheetId = googleSheetsService.createCourseSpreadsheet("test sheet").get()?.long
    assertNotNull(spreadsheetId)

    googleSheetsService.updateRating(
      spreadsheetId,
      courseStorage.resolveCourse(course1Id).value,
      assignmentStorage.getAssignmentsForCourse(course1Id).value,
      problemStorage.getProblemsFromCourse(course1Id).value,
      courseStorage.getStudents(course1Id).value,
      gradeTable.getCourseRating(course1Id).value,
    )
    googleSheetsService.updateRating(
      spreadsheetId,
      courseStorage.resolveCourse(course2Id).value,
      assignmentStorage.getAssignmentsForCourse(course2Id).value,
      problemStorage.getProblemsFromCourse(course2Id).value,
      courseStorage.getStudents(course2Id).value,
      gradeTable.getCourseRating(course2Id).value,
    )
  }
}
