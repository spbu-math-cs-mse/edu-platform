package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
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

  private val coursesDistributor = DatabaseCoursesDistributor(database)
  private val gradeTable = DatabaseGradeTable(database)
  private val studentStorage = DatabaseStudentStorage(database)
  private val teacherStorage = DatabaseTeacherStorage(database)
  private val solutionDistributor = DatabaseSolutionDistributor(database)
  private val problemStorage = DatabaseProblemStorage(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)
  private val googleSheetsService =
    GoogleSheetsServiceImpl(config.googleSheetsConfig.serviceAccountKey)

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
  }

  //  @Ignore
  @Test
  fun `update rating works`() {
    val course1Id = coursesDistributor.createCourse("course 1")
    val course2Id = coursesDistributor.createCourse("course 2")
    val student1Id = studentStorage.createStudent()
    val student2Id = studentStorage.createStudent()
    val student3Id = studentStorage.createStudent()
    coursesDistributor.addStudentToCourse(student1Id, course1Id)
    coursesDistributor.addStudentToCourse(student1Id, course2Id)
    coursesDistributor.addStudentToCourse(student2Id, course1Id)
    coursesDistributor.addStudentToCourse(student2Id, course2Id)
    coursesDistributor.addStudentToCourse(student3Id, course1Id)

    val teacher1Id = teacherStorage.createTeacher()
    coursesDistributor.addTeacherToCourse(teacher1Id, course1Id)
    coursesDistributor.addTeacherToCourse(teacher1Id, course2Id)

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
      solutionDistributor.inputSolution(
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
      solutionDistributor.inputSolution(
        student2Id,
        RawChatId(0),
        MessageId(0),
        TextWithMediaAttachments(),
        ProblemId(problemId.toLong() * 2),
        LocalDateTime.now(),
        teacher1Id,
      )
    }

    for (solutionId in 1..17) {
      val solution = solutionDistributor.querySolution(teacher1Id).get()
      assertNotNull(solution)
      gradeTable.recordSolutionAssessment(
        solution.id,
        teacher1Id,
        SolutionAssessment(solutionId % 2, TextWithMediaAttachments("comment")),
      )
    }

    googleSheetsService.updateRating(
      config.googleSheetsConfig.spreadsheetId,
      coursesDistributor.resolveCourse(course1Id).value,
      assignmentStorage.getAssignmentsForCourse(course1Id),
      problemStorage.getProblemsFromCourse(course1Id),
      coursesDistributor.getStudents(course1Id),
      gradeTable.getCourseRating(course1Id),
    )
    googleSheetsService.updateRating(
      config.googleSheetsConfig.spreadsheetId,
      coursesDistributor.resolveCourse(course2Id).value,
      assignmentStorage.getAssignmentsForCourse(course2Id),
      problemStorage.getProblemsFromCourse(course2Id),
      coursesDistributor.getStudents(course2Id),
      gradeTable.getCourseRating(course2Id),
    )
  }
}
