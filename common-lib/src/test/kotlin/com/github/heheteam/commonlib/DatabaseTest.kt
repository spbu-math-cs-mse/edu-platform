package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCoursesDistributor
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseSolutionDistributor
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.util.MonotoneDummyClock
import com.github.heheteam.commonlib.util.fillWithSamples
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.toJavaLocalDateTime
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
  private val problemStorage = DatabaseProblemStorage(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)

  private val emptyContent = SolutionContent()
  private val good = SolutionAssessment(1)
  private val bad = SolutionAssessment(0)

  private fun createAssignment(courseId: CourseId): List<Problem> {
    val assignment =
      assignmentStorage.createAssignment(
        courseId,
        "",
        (1..5).map { ProblemDescription(it, it.toString()) },
      )
    return problemStorage.getProblemsFromAssignment(assignment)
  }

  private fun inputSampleSolution(
    studentId: StudentId,
    chatId: RawChatId,
    problem: Problem,
    clock: MonotoneDummyClock,
    teacherId: TeacherId,
  ) =
    solutionDistributor.inputSolution(
      studentId,
      chatId,
      MessageId(problem.id.id),
      SolutionContent(text = "sample${problem.number}"),
      problem.id,
      clock.next().toJavaLocalDateTime(),
      teacherId,
    )

  private fun createCourseWithTeacherAndStudent(): Triple<CourseId, TeacherId, StudentId> {
    val courseId = coursesDistributor.createCourse("sample course")
    val teacherId = teacherStorage.createTeacher()
    val studentId = studentStorage.createStudent()
    coursesDistributor.addStudentToCourse(studentId, courseId)
    coursesDistributor.addTeacherToCourse(teacherId, courseId)
    return Triple(courseId, teacherId, studentId)
  }

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
  fun `query solution returns last unchecked solution`() {
    val chatId = RawChatId(0)
    val clock = MonotoneDummyClock()
    val (courseId, teacherId, studentId) = createCourseWithTeacherAndStudent()
    val problemsInCourse = createAssignment(courseId)
    val solutions =
      problemsInCourse.map { problem ->
        inputSampleSolution(studentId, chatId, problem, clock, teacherId)
      }

    gradeTable.recordSolutionAssessment(solutions[0], teacherId, SolutionAssessment(0))
    gradeTable.recordSolutionAssessment(solutions[2], teacherId, SolutionAssessment(0))
    gradeTable.recordSolutionAssessment(solutions[3], teacherId, SolutionAssessment(0))

    val solutionId1 = solutionDistributor.querySolution(teacherId)
    assertEquals(solutions[1], solutionId1.value!!.id)
    gradeTable.recordSolutionAssessment(solutions[1], teacherId, SolutionAssessment(0))

    val solutionId2 = solutionDistributor.querySolution(teacherId)
    assertEquals(solutions[4], solutionId2.value!!.id)
    gradeTable.recordSolutionAssessment(solutions[4], teacherId, SolutionAssessment(0))

    assertEquals(null, solutionDistributor.querySolution(teacherId).value)
  }

  @Test
  fun `query solution from course returns last unchecked solution`() {
    val chatId = RawChatId(0)
    val clock = MonotoneDummyClock()
    val (courseId, teacherId, studentId) = createCourseWithTeacherAndStudent()

    val solutions =
      createAssignment(courseId).map { problem ->
        inputSampleSolution(studentId, chatId, problem, clock, teacherId)
      }

    gradeTable.recordSolutionAssessment(solutions[0], teacherId, bad)
    gradeTable.recordSolutionAssessment(solutions[2], teacherId, bad)
    gradeTable.recordSolutionAssessment(solutions[3], teacherId, bad)

    val solutionId1 = solutionDistributor.querySolution(courseId)
    assertEquals(solutions[1], solutionId1.value!!.id)
    gradeTable.recordSolutionAssessment(solutions[1], teacherId, bad)

    val solutionId2 = solutionDistributor.querySolution(courseId)
    assertEquals(solutions[4], solutionId2.value!!.id)
    gradeTable.recordSolutionAssessment(solutions[4], teacherId, bad)

    assertEquals(null, solutionDistributor.querySolution(courseId).value)
  }

  @Test
  fun `grade table properly returns all gradings`() {
    val clock = MonotoneDummyClock()
    val (teachers, solution) = generateSampleTeachersAndSolution()
    val expected = mutableSetOf<GradingEntry>()
    for ((i, teacher) in teachers.withIndex()) {
      val assessment = if (i % 2 == 0) good else bad
      val timestamp = clock.next()
      gradeTable.recordSolutionAssessment(solution, teacher, assessment, timestamp)
      expected.add(GradingEntry(teacher, assessment, timestamp))
    }
    val gradingEntries = gradeTable.getGradingsForSolution(solution)
    assertEquals(expected, gradingEntries.toSet())
  }

  private fun generateSampleTeachersAndSolution(): Pair<List<TeacherId>, SolutionId> {
    val content =
      fillWithSamples(
        coursesDistributor,
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
