package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.ProblemGrade
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.SolutionId
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
import com.github.heheteam.commonlib.util.fillWithSamples
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

  private fun createAssignment(courseId: CourseId): AssignmentId = assignmentStorage.createAssignment(
    courseId,
    "assignment",
    listOf(
      ProblemDescription(1, "p1", "", 1),
      ProblemDescription(3, "p2", "", 1),
      ProblemDescription(2, "p3", "", 1),
      ProblemDescription(4, "p4", "", 1),
    ),
    problemStorage,
  )

  @Test
  fun `student performance for assignment`() {
    val courseId = coursesDistributor.createCourse("course 1")
    val studentId = studentStorage.createStudent()
    coursesDistributor.addStudentToCourse(studentId, courseId)
    val teacherId = teacherStorage.createTeacher()
    coursesDistributor.addTeacherToCourse(teacherId, courseId)
    val assignmentId = createAssignment(courseId)

    for (problemId in 1L..3L) {
      solutionDistributor.inputSolution(
        studentId,
        RawChatId(0),
        MessageId(0),
        SolutionContent(),
        ProblemId(problemId),
      )
    }

    val solution1 = solutionDistributor.querySolution(teacherId).value
    assertNotNull(solution1)
    gradeTable.recordSolutionAssessment(solution1.id, teacherId, SolutionAssessment(1, "comment"))
    gradeTable.recordSolutionAssessment(solution1.id, teacherId, SolutionAssessment(0, "comment"))

    val solution2 = solutionDistributor.querySolution(teacherId).value
    assertNotNull(solution2)
    gradeTable.recordSolutionAssessment(solution2.id, teacherId, SolutionAssessment(1, "comment"))

    val performance = gradeTable.getStudentPerformance(studentId, assignmentId).map { it.first.id to it.second }

    assertEquals(ProblemId(1) to 0.toGraded(), performance[0])
    assertEquals(ProblemId(2) to 1.toGraded(), performance[1])
    assertEquals(performance[2].first, ProblemId(3))
    assertTrue(performance[2].second is ProblemGrade.Unchecked)
    assertEquals(performance[3].first, ProblemId(4))
    assertTrue(performance[3].second is ProblemGrade.Unsent)
  }

  private val emptyContent = SolutionContent()
  private val defaultTimestamp = LocalDateTime.of(2000, 1, 1, 12, 0)

  private val good = SolutionAssessment(1)
  private val bad = SolutionAssessment(0)

  @Test
  fun `grade table properly returns all gradings`() {
    val (teachers, solution) = generateSampleTeachersAndSolution()
    val expected = mutableSetOf<GradingEntry>()
    for ((i, teacher) in teachers.withIndex()) {
      val assessment = if (i % 2 == 0) good else bad
      val timestamp = defaultTimestamp.plusMinutes(i.toLong())
      gradeTable.recordSolutionAssessment(
        solution,
        teacher,
        assessment,
        timestamp.toKotlinLocalDateTime(),
      )
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
