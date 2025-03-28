package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.api.GradingEntry
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
