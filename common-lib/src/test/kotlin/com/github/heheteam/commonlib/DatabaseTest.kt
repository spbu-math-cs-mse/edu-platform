package com.github.heheteam.commonlib

import com.github.heheteam.commonlib.database.DatabaseAdminStorage
import com.github.heheteam.commonlib.database.DatabaseAssignmentStorage
import com.github.heheteam.commonlib.database.DatabaseCourseStorage
import com.github.heheteam.commonlib.database.DatabaseGradeTable
import com.github.heheteam.commonlib.database.DatabaseProblemStorage
import com.github.heheteam.commonlib.database.DatabaseStudentStorage
import com.github.heheteam.commonlib.database.DatabaseSubmissionDistributor
import com.github.heheteam.commonlib.database.DatabaseTeacherStorage
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.SubmissionId
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

  private val courseStorage = DatabaseCourseStorage(database)
  private val gradeTable = DatabaseGradeTable(database)
  private val adminStorage = DatabaseAdminStorage(database)
  private val studentStorage = DatabaseStudentStorage(database)
  private val teacherStorage = DatabaseTeacherStorage(database)
  private val submissionDistributor = DatabaseSubmissionDistributor(database)
  private val problemStorage = DatabaseProblemStorage(database)
  private val assignmentStorage = DatabaseAssignmentStorage(database, problemStorage)

  private val emptyContent = TextWithMediaAttachments()
  private val good = SubmissionAssessment(1)
  private val bad = SubmissionAssessment(0)

  private fun createAssignment(courseId: CourseId): List<Problem> {
    val assignment =
      assignmentStorage
        .createAssignment(courseId, "", (1..5).map { ProblemDescription(it, it.toString()) })
        .value
    return problemStorage.getProblemsFromAssignment(assignment).value
  }

  private fun inputSampleSubmission(
    studentId: StudentId,
    chatId: RawChatId,
    problem: Problem,
    clock: MonotoneDummyClock,
    teacherId: TeacherId,
  ) =
    submissionDistributor.inputSubmission(
      studentId,
      chatId,
      MessageId(problem.id.long),
      TextWithMediaAttachments(text = "sample${problem.number}"),
      problem.id,
      clock.next().toJavaLocalDateTime(),
      teacherId,
    )

  private fun createCourseWithTeacherAndStudent(): Triple<CourseId, TeacherId, StudentId> {
    val courseId = courseStorage.createCourse("sample course").value
    val teacherId = teacherStorage.createTeacher()
    val studentId = studentStorage.createStudent()
    courseStorage.addStudentToCourse(studentId, courseId)
    courseStorage.addTeacherToCourse(teacherId, courseId)
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
    val id = courseStorage.createCourse(sampleDescription).value
    val requiredId = courseStorage.getCourses().value.single().id
    assertEquals(id, requiredId)
    val resolvedCourse = courseStorage.resolveCourse(requiredId)
    assertEquals(true, resolvedCourse.isOk)
    assertEquals(sampleDescription, resolvedCourse.value.name)
  }

  @Test
  fun `query submission returns last unchecked submission`() {
    val chatId = RawChatId(0)
    val clock = MonotoneDummyClock()
    val (courseId, teacherId, studentId) = createCourseWithTeacherAndStudent()
    val problemsInCourse = createAssignment(courseId)
    val submissions =
      problemsInCourse.map { problem ->
        inputSampleSubmission(studentId, chatId, problem, clock, teacherId)
      }

    gradeTable.recordSubmissionAssessment(submissions[0], teacherId, SubmissionAssessment(0))
    gradeTable.recordSubmissionAssessment(submissions[2], teacherId, SubmissionAssessment(0))
    gradeTable.recordSubmissionAssessment(submissions[3], teacherId, SubmissionAssessment(0))

    val submissionId1 = submissionDistributor.querySubmission(teacherId)
    assertEquals(submissions[1], submissionId1.value!!.id)
    gradeTable.recordSubmissionAssessment(submissions[1], teacherId, SubmissionAssessment(0))

    val submissionId2 = submissionDistributor.querySubmission(teacherId)
    assertEquals(submissions[4], submissionId2.value!!.id)
    gradeTable.recordSubmissionAssessment(submissions[4], teacherId, SubmissionAssessment(0))

    assertEquals(null, submissionDistributor.querySubmission(teacherId).value)
  }

  @Test
  fun `query submission from course returns last unchecked submission`() {
    val chatId = RawChatId(0)
    val clock = MonotoneDummyClock()
    val (courseId, teacherId, studentId) = createCourseWithTeacherAndStudent()

    val submissions =
      createAssignment(courseId).map { problem ->
        inputSampleSubmission(studentId, chatId, problem, clock, teacherId)
      }

    gradeTable.recordSubmissionAssessment(submissions[0], teacherId, bad)
    gradeTable.recordSubmissionAssessment(submissions[2], teacherId, bad)
    gradeTable.recordSubmissionAssessment(submissions[3], teacherId, bad)

    val submissionId1 = submissionDistributor.querySubmission(courseId)
    assertEquals(submissions[1], submissionId1.value!!.id)
    gradeTable.recordSubmissionAssessment(submissions[1], teacherId, bad)

    val submissionId2 = submissionDistributor.querySubmission(courseId)
    assertEquals(submissions[4], submissionId2.value!!.id)
    gradeTable.recordSubmissionAssessment(submissions[4], teacherId, bad)

    assertEquals(null, submissionDistributor.querySubmission(courseId).value)
  }

  @Test
  fun `grade table properly returns all gradings`() {
    val clock = MonotoneDummyClock()
    val (teachers, submission) = generateSampleTeachersAndSubmission()
    val expected = mutableSetOf<GradingEntry>()
    for ((i, teacher) in teachers.withIndex()) {
      val assessment = if (i % 2 == 0) good else bad
      val timestamp = clock.next()
      gradeTable.recordSubmissionAssessment(submission, teacher, assessment, timestamp)
      expected.add(GradingEntry(teacher, assessment, timestamp))
    }
    val gradingEntries = gradeTable.getGradingsForSubmission(submission).value
    assertEquals(expected, gradingEntries.toSet())
  }

  private fun generateSampleTeachersAndSubmission(): Pair<List<TeacherId>, SubmissionId> {
    val content =
      fillWithSamples(
        courseStorage,
        assignmentStorage,
        adminStorage,
        studentStorage,
        teacherStorage,
        database,
        initTeachers = true,
      )
    val someAssignment =
      assignmentStorage.getAssignmentsForCourse(content.courses.realAnalysis).value.first()
    val someProblem = problemStorage.getProblemsFromAssignment(someAssignment.id).value.first()
    val someStudent = content.students[0]
    val teachers = content.teachers
    val submission =
      submissionDistributor.inputSubmission(
        someStudent,
        RawChatId(0L),
        MessageId(0L),
        emptyContent,
        someProblem.id,
      )
    return Pair(teachers, submission)
  }
}
