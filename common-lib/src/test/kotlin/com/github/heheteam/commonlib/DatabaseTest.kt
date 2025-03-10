package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.util.fillWithSamples
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database

class DatabaseTest {
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
      assignmentStorage.createAssignment(
        course1Id,
        "assignment 1",
        listOf(
          ProblemDescription(1, "p1", "", 1),
          ProblemDescription(2, "p2", "", 1),
          ProblemDescription(3, "p3", "", 1),
        ),
        problemStorage,
      )
    val assignment2Id =
      assignmentStorage.createAssignment(
        course1Id,
        "assignment 2",
        listOf(
          ProblemDescription(1, "p1", "", 1),
          ProblemDescription(2, "p2", "", 1),
          ProblemDescription(3, "p3", "", 1),
        ),
        problemStorage,
      )
    val assignment3Id =
      assignmentStorage.createAssignment(
        course2Id,
        "assignment 3",
        listOf(ProblemDescription(1, "p1", "", 1), ProblemDescription(2, "p2", "", 1)),
        problemStorage,
      )

    for (problemId in 1..8) {
      solutionDistributor.inputSolution(
        student1Id,
        RawChatId(0),
        MessageId(0),
        SolutionContent(),
        ProblemId(problemId.toLong()),
      )
    }
    for (problemId in 1..4) {
      val id =
        solutionDistributor.inputSolution(
          student2Id,
          RawChatId(0),
          MessageId(0),
          SolutionContent(),
          ProblemId(problemId.toLong()),
        )
      assertEquals(id.id, problemId + 8L)
    }

    repeat(10) {
      val solution = solutionDistributor.querySolution(teacher1Id).value
      assertNotNull(solution)
      gradeTable.recordSolutionAssessment(solution.id, teacher1Id, SolutionAssessment(1, "comment"))
    }
    repeat(2) {
      val solution = solutionDistributor.querySolution(teacher1Id).value
      assertNotNull(solution)
      gradeTable.recordSolutionAssessment(solution.id, teacher1Id, SolutionAssessment(0, "comment"))
    }
    val gradesS1A1 = mapOf(ProblemId(1) to 1, ProblemId(2) to 1, ProblemId(3) to 1)
    assertEquals(gradesS1A1, gradeTable.getStudentPerformance(student1Id, listOf(assignment1Id)))
    val gradesS1A2 = mapOf(ProblemId(4) to 1, ProblemId(5) to 1, ProblemId(6) to 1)
    assertEquals(gradesS1A2, gradeTable.getStudentPerformance(student1Id, listOf(assignment2Id)))
    val gradesS1A3 = mapOf(ProblemId(7) to 1, ProblemId(8) to 1)
    assertEquals(gradesS1A3, gradeTable.getStudentPerformance(student1Id, listOf(assignment3Id)))

    assertEquals(gradesS1A1 + gradesS1A2 + gradesS1A3, gradeTable.getStudentPerformance(student1Id))

    val gradesS2A1 = mapOf(ProblemId(1) to 1, ProblemId(2) to 1, ProblemId(3) to 0)
    assertEquals(gradesS2A1, gradeTable.getStudentPerformance(student2Id, listOf(assignment1Id)))
    val gradesS2A2 = mapOf(ProblemId(4) to 0)
    assertEquals(gradesS2A2, gradeTable.getStudentPerformance(student2Id, listOf(assignment2Id)))
    val gradesS2A3 = mapOf<ProblemId, Grade>()
    assertEquals(gradesS2A3, gradeTable.getStudentPerformance(student2Id, listOf(assignment3Id)))

    assertEquals(gradesS2A1 + gradesS2A2 + gradesS2A3, gradeTable.getStudentPerformance(student2Id))
  }

  val emptyContent = SolutionContent()
  val defaultTimestamp: LocalDateTime = LocalDateTime.of(2000, 1, 1, 12, 0)

  val good = SolutionAssessment(1)
  val bad = SolutionAssessment(0)

  @Test
  fun `grade table properly returns all gradings`() {
    val (teachers, solution) = generateSampleTeachersAndSolution()
    val expected = mutableSetOf<GradingEntry>()
    for ((i, teacher) in teachers.withIndex()) {
      val assessment = if (i % 2 == 0) good else bad
      val timestamp = defaultTimestamp.plusMinutes(i.toLong())
      gradeTable.recordSolutionAssessment(solution, teacher, assessment, timestamp)
      expected.add(GradingEntry(teacher, assessment, timestamp.toKotlinLocalDateTime()))
    }
    val gradingEntries = gradeTable.getGradingsForSolution(solution)
    assertEquals(expected, gradingEntries.toSet())
  }

  private fun generateSampleTeachersAndSolution(): Pair<List<TeacherId>, SolutionId> {
    val content =
      fillWithSamples(
        coursesDistributor,
        problemStorage,
        assignmentStorage,
        studentStorage,
        teacherStorage,
        database,
      )
    val someAssignment = assignmentStorage.getAssignmentsForCourse(content.courses.first()).first()
    val someProblem = problemStorage.getProblemsFromAssignment(someAssignment.id).first()
    val someStudent = content.students[0]
    val teachers = content.teachers
    val solution =
      solutionDistributor.inputSolution(
        someStudent,
        RawChatId(0L),
        MessageId(0L),
        emptyContent,
        someProblem.id,
      )
    return Pair(teachers, solution)
  }
}
