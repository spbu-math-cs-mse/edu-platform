import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionType
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.database.*
import com.github.heheteam.commonlib.mock.InMemoryTeacherStatistics
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import org.jetbrains.exposed.sql.Database
import kotlin.test.*

class DatabaseTest {
  val database =
    Database.connect(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
      driver = "org.h2.Driver",
    )

  private val coursesDistributor = DatabaseCoursesDistributor(database)
  private val gradeTable = DatabaseGradeTable(database)
  private val studentStorage = DatabaseStudentStorage(database)
  private val teacherStorage = DatabaseTeacherStorage(database)
  private val solutionDistributor = DatabaseSolutionDistributor(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database)
  private val problemStorage = DatabaseProblemStorage(database)
  private val teacherStatistics = InMemoryTeacherStatistics()

  @BeforeTest
  @AfterTest
  fun setup() {
    reset(database)
  }

  @Test
  fun `course distributor works`() {
    val sampleDescription = "sample description"
    val id = coursesDistributor.createCourse(sampleDescription)
    val requiredId = coursesDistributor.getCourses().single().id
    assertEquals(id, requiredId)
    val resolvedCourse = coursesDistributor.resolveCourse(requiredId)
    assertEquals(true, resolvedCourse.isOk)
    assertEquals(sampleDescription, resolvedCourse.value.name)
  }

  @Test
  fun `student performance works`() {
    val course1Id = coursesDistributor.createCourse("course 1")
    val course2Id = coursesDistributor.createCourse("course 2")
    val student1Id = studentStorage.createStudent()
    val student2Id = studentStorage.createStudent()
    coursesDistributor.addStudentToCourse(student1Id, course1Id)
    coursesDistributor.addStudentToCourse(student1Id, course2Id)
    coursesDistributor.addStudentToCourse(student2Id, course1Id)

    val teacher1Id = teacherStorage.createTeacher()
    coursesDistributor.addTeacherToCourse(teacher1Id, course1Id)
    coursesDistributor.addTeacherToCourse(teacher1Id, course2Id)

    val assignment1Id =
      assignmentStorage.createAssignment(course1Id, "assignment 1", listOf("p1", "p2", "p3"), problemStorage)
    val assignment2Id =
      assignmentStorage.createAssignment(course1Id, "assignment 2", listOf("p1", "p2", "p3"), problemStorage)
    val assignment3Id =
      assignmentStorage.createAssignment(course2Id, "assignment 3", listOf("p1", "p2"), problemStorage)

    for (problemId in 1..8) {
      solutionDistributor.inputSolution(
        student1Id,
        RawChatId(0),
        MessageId(0),
        SolutionContent(listOf(), "", SolutionType.TEXT),
        ProblemId(problemId.toLong()),
      )
    }
    for (problemId in 1..4) {
      val id =
        solutionDistributor.inputSolution(
          student2Id,
          RawChatId(0),
          MessageId(0),
          SolutionContent(listOf(), "", SolutionType.TEXT),
          ProblemId(problemId.toLong()),
        )
      assertEquals(id.id, problemId + 8L)
    }

    repeat(10) {
      val solution = solutionDistributor.querySolution(teacher1Id, gradeTable)
      assertNotNull(solution)
      gradeTable.assessSolution(
        solution.id,
        teacher1Id,
        SolutionAssessment(1, "comment"),
        gradeTable,
        teacherStatistics,
      )
    }
    repeat(2) {
      val solution = solutionDistributor.querySolution(teacher1Id, gradeTable)
      assertNotNull(solution)
      gradeTable.assessSolution(
        solution.id,
        teacher1Id,
        SolutionAssessment(0, "comment"),
        gradeTable,
        teacherStatistics,
      )
    }
    val gradesS1A1 = mapOf(ProblemId(1) to 1, ProblemId(2) to 1, ProblemId(3) to 1)
    assertEquals(gradesS1A1, gradeTable.getStudentPerformance(student1Id, assignment1Id, solutionDistributor))
    val gradesS1A2 = mapOf(ProblemId(4) to 1, ProblemId(5) to 1, ProblemId(6) to 1)
    assertEquals(gradesS1A2, gradeTable.getStudentPerformance(student1Id, assignment2Id, solutionDistributor))
    val gradesS1A3 = mapOf(ProblemId(7) to 1, ProblemId(8) to 1)
    assertEquals(gradesS1A3, gradeTable.getStudentPerformance(student1Id, assignment3Id, solutionDistributor))

    assertEquals(
      gradesS1A1 + gradesS1A2 + gradesS1A3,
      gradeTable.getStudentPerformance(student1Id, solutionDistributor),
    )

    val gradesS2A1 = mapOf(ProblemId(1) to 1, ProblemId(2) to 1, ProblemId(3) to 0)
    assertEquals(gradesS2A1, gradeTable.getStudentPerformance(student2Id, assignment1Id, solutionDistributor))
    val gradesS2A2 = mapOf(ProblemId(4) to 0)
    assertEquals(gradesS2A2, gradeTable.getStudentPerformance(student2Id, assignment2Id, solutionDistributor))
    val gradesS2A3 = mapOf<ProblemId, Grade>()
    assertEquals(gradesS2A3, gradeTable.getStudentPerformance(student2Id, assignment3Id, solutionDistributor))

    assertEquals(
      gradesS2A1 + gradesS2A2 + gradesS2A3,
      gradeTable.getStudentPerformance(student2Id, solutionDistributor),
    )
  }
}
