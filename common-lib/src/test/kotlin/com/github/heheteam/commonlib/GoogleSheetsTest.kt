package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.googlesheets.GoogleSheetsService
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import com.github.michaelbull.result.get
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

// @Ignore
class GoogleSheetsTest {
  private val config = loadConfig()

  private val database = Database.connect(
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
  private val teacherStatistics = InMemoryTeacherStatistics()

  private val googleSheetsService = GoogleSheetsService(
    config.googleSheetsConfig.serviceAccountKey,
    config.googleSheetsConfig.spreadsheetId,
  )

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
      listOf(ProblemDescription("p1"), ProblemDescription("p2"), ProblemDescription("p3")),
      problemStorage
    )
    assignmentStorage.createAssignment(
      course1Id,
      "assignment 2",
      listOf(ProblemDescription("p1"), ProblemDescription("p2"), ProblemDescription("p3")),
      problemStorage
    )
    assignmentStorage.createAssignment(
      course1Id,
      "assignment 3",
      listOf(ProblemDescription("p1"), ProblemDescription("p2"), ProblemDescription("p3")),
      problemStorage
    )
    assignmentStorage.createAssignment(
      course2Id,
      "assignment 1",
      listOf(
        ProblemDescription("p1"),
        ProblemDescription("p2"),
        ProblemDescription("p3"),
        ProblemDescription("p4")
      ),
      problemStorage
    )

    for (problemId in 1..11) {
      solutionDistributor.inputSolution(
        student1Id,
        RawChatId(0),
        MessageId(0),
        SolutionContent(listOf(), "", SolutionType.TEXT),
        ProblemId(problemId.toLong()),
      )
    }
    for (problemId in 1..6) {
      solutionDistributor.inputSolution(
        student2Id,
        RawChatId(0),
        MessageId(0),
        SolutionContent(listOf(), "", SolutionType.TEXT),
        ProblemId(problemId.toLong() * 2),
      )
    }

    for (solutionId in 1..17) {
      val solution = solutionDistributor.querySolution(teacher1Id, gradeTable)
      assertNotNull(solution)
      gradeTable.assessSolution(
        solution.get()!!.id,
        teacher1Id,
        SolutionAssessment(solutionId % 2, "comment"),
        teacherStatistics,
      )
    }

    googleSheetsService.updateRating(
      coursesDistributor.resolveCourse(course1Id).value,
      assignmentStorage.getAssignmentsForCourse(course1Id),
      problemStorage.getProblemsFromCourse(course1Id),
      coursesDistributor.getStudents(course1Id),
      gradeTable.getCourseRating(course1Id, solutionDistributor),
    )
    googleSheetsService.updateRating(
      coursesDistributor.resolveCourse(course2Id).value,
      assignmentStorage.getAssignmentsForCourse(course2Id),
      problemStorage.getProblemsFromCourse(course2Id),
      coursesDistributor.getStudents(course2Id),
      gradeTable.getCourseRating(course2Id, solutionDistributor),
    )
  }
}
