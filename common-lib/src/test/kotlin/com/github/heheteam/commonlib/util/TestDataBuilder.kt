package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Submission
import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.SubmissionInputRequest
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.TelegramMessageContent
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.ApiCollection
import com.github.heheteam.commonlib.interfaces.AdminId
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.ScheduledMessageId
import com.github.heheteam.commonlib.logic.SubmissionSendingResult
import com.github.michaelbull.result.binding
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

data class MonotoneCounter(private var startValue: Long = 0) {
  fun next(): Long = ++startValue
}

class TestDataBuilder(internal val apis: ApiCollection) {
  private val clock = MonotoneDummyClock()
  private val defaultTimestamp = clock.next()
  private val chatIdCounter = MonotoneCounter()
  private val defaultChatId = chatIdCounter.next()
  private val messageIdCounter = MonotoneCounter()
  private val defaultMessageId = MessageId(messageIdCounter.next())
  private val coursesChatId = mutableMapOf<CourseId, RawChatId>()

  fun whitelistAdmin(tgId: Long): Long {
    apis.adminApi.addTgIdToWhitelist(tgId.toChatId())
    return tgId
  }

  fun admin(name: String, surname: String, tgId: Long = defaultChatId): Admin =
    binding {
      apis.adminApi.addTgIdToWhitelist(tgId.toChatId())
      val adminId = apis.adminApi.createAdmin(name, surname, tgId).bind()
      val admin = Admin(adminId, name, surname, tgId.toRawChatId())
      admin
    }
      .value

  fun student(name: String, surname: String, tgId: Long = defaultChatId): Student =
    binding {
      val studentId = apis.studentApi.createStudent(name, surname, tgId).value
      val student = apis.studentApi.loginById(studentId).bind()
      student
    }
      .value

  fun teacher(name: String, surname: String, tgId: Long = defaultChatId): Teacher =
    binding {
      val teacherId = apis.teacherApi.createTeacher(name, surname, tgId)
      val teacher = apis.teacherApi.loginById(teacherId).bind()
      teacher
    }
      .value

  suspend fun course(name: String, setup: suspend CourseContext.() -> Unit = {}): Course {
    val courseId = apis.adminApi.createCourse(name).value
    setup.invoke(CourseContext(courseId))
    val course = apis.adminApi.getCourse(name).value!!
    return course
  }

  inner class CourseContext(private val courseId: CourseId) {
    fun setChat(tgId: Long = defaultChatId) {
      apis.teacherApi.setCourseGroup(courseId, tgId.toRawChatId())
      coursesChatId[courseId] = tgId.toRawChatId()
    }

    fun withStudent(student: Student) {
      apis.studentApi.applyForCourse(student.id, courseId)
    }

    fun withTeacher(teacher: Teacher) {
      apis.adminApi.registerTeacherForCourse(teacher.id, courseId)
    }

    fun assignment(
      description: String,
      setup: AssignmentContext.() -> Unit = {},
    ): Pair<Assignment, List<Problem>> {
      val assignmentContext = AssignmentContext().apply(setup)
      val assignmentId =
        apis.adminApi.createAssignment(courseId, description, assignmentContext.problems).value
      val assignment =
        apis.studentApi.getCourseAssignments(courseId).value.first { it.id == assignmentId }
      return assignment to apis.studentApi.getProblemsFromAssignment(assignmentId).value
    }
  }

  inner class AssignmentContext {
    val problems = mutableListOf<ProblemDescription>()

    fun problem(description: String, maxScore: Int, deadline: LocalDateTime? = null) {
      problems.add(
        ProblemDescription(
          number = (problems.size + 1).toString(),
          description = description,
          maxScore = maxScore,
          serialNumber = problems.size,
          deadline = deadline,
        )
      )
    }
  }

  suspend fun submission(student: Student, problem: Problem, content: String): Submission {
    val submissionInputRequest =
      SubmissionInputRequest(
        studentId = student.id,
        problemId = problem.id,
        submissionContent = TextWithMediaAttachments(content),
        telegramMessageInfo = TelegramMessageInfo(student.tgId, defaultMessageId),
        timestamp = defaultTimestamp,
      )
    val submissionResult =
      apis.studentApi.inputSubmission(submissionInputRequest) as SubmissionSendingResult.Success
    return Submission(
      submissionResult.submissionId,
      submissionInputRequest.studentId,
      submissionInputRequest.telegramMessageInfo.chatId,
      submissionInputRequest.telegramMessageInfo.messageId,
      submissionInputRequest.problemId,
      submissionInputRequest.submissionContent,
      null,
      submissionInputRequest.timestamp,
    )
  }

  suspend fun assessment(
    teacher: Teacher,
    submission: Submission,
    grade: Int,
  ): SubmissionAssessment {
    val assessment = SubmissionAssessment(grade, TextWithMediaAttachments())
    apis.teacherApi.assessSubmission(
      submissionId = submission.id,
      teacherId = teacher.id,
      submissionAssessment = assessment,
      timestamp = defaultTimestamp,
    )
    return assessment
  }

  suspend fun movingDeadlinesRequest(student: Student, newDeadline: LocalDateTime) {
    apis.studentApi.requestReschedulingDeadlines(student.id, newDeadline)
  }

  suspend fun moveDeadlines(student: Student, newDeadline: LocalDateTime) {
    apis.adminApi.moveAllDeadlinesForStudent(student.id, newDeadline)
  }

  fun sendScheduledMessage(
    adminId: AdminId,
    timestamp: LocalDateTime,
    content: TelegramMessageContent,
    shortName: String,
    courseId: CourseId,
  ) =
    apis.adminApi.sendScheduledMessage(
      adminId,
      timestamp.toJavaLocalDateTime(),
      content,
      shortName,
      courseId,
    )

  fun resolveScheduledMessage(scheduledMessageId: ScheduledMessageId) =
    apis.adminApi.resolveScheduledMessage(scheduledMessageId)

  suspend fun checkAndSentMessages(timestamp: LocalDateTime) =
    apis.studentApi.checkAndSendMessages(timestamp)

  fun viewRecordedMessages(adminId: AdminId? = null, courseId: CourseId? = null, limit: Int = 5) =
    apis.adminApi.viewScheduledMessages(adminId, courseId, limit)

  suspend fun deleteScheduledMessage(scheduledMessageId: ScheduledMessageId) =
    apis.adminApi.deleteScheduledMessage(scheduledMessageId)

  fun submissionInputRequest(
    student: Student,
    problem: Problem,
    content: String,
  ): SubmissionInputRequest =
    SubmissionInputRequest(
      studentId = student.id,
      problemId = problem.id,
      submissionContent = TextWithMediaAttachments(content),
      telegramMessageInfo = TelegramMessageInfo(student.tgId, defaultMessageId),
      timestamp = defaultTimestamp,
    )
}

suspend fun buildData(apis: ApiCollection, block: suspend TestDataBuilder.() -> Unit) {
  TestDataBuilder(apis).block()
}
