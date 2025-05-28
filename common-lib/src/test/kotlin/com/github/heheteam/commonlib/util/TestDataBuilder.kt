package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.Admin
import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.api.ApiCollection
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.michaelbull.result.binding
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.datetime.LocalDateTime

data class MonotoneCounter(private var startValue: Long = 0) {
  fun next(): Long = ++startValue
}

class TestDataBuilder(private val apis: ApiCollection) {
  private val clock = MonotoneDummyClock()
  private val defaultTimestamp = clock.next()
  private val chatIdCounter = MonotoneCounter()
  private val defaultChatId = chatIdCounter.next()
  private val messageIdCounter = MonotoneCounter()
  private val defaultMessageId = MessageId(messageIdCounter.next())
  private val coursesChatId = mutableMapOf<CourseId, RawChatId>()

  fun admin(name: String, surname: String, tgId: Long = defaultChatId): Admin =
    binding {
        val adminId = apis.adminApi.createAdmin(name, surname, tgId)
        val admin = apis.adminApi.loginById(adminId).bind()
        admin
      }
      .value

  fun student(name: String, surname: String, tgId: Long = defaultChatId): Student =
    binding {
        val studentId = apis.studentApi.createStudent(name, surname, tgId)
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

  fun course(name: String, setup: CourseContext.() -> Unit = {}): Course {
    val courseId = apis.adminApi.createCourse(name)
    CourseContext(courseId).apply(setup)
    val course = apis.adminApi.getCourse(name)!!
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
        apis.adminApi.createAssignment(courseId, description, assignmentContext.problems)
      val assignment =
        apis.studentApi.getCourseAssignments(courseId).first { it.id == assignmentId }
      return assignment to apis.studentApi.getProblemsFromAssignment(assignmentId)
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

  fun solution(student: Student, problem: Problem, content: String): Solution {
    val solutionInputRequest =
      SolutionInputRequest(
        studentId = student.id,
        problemId = problem.id,
        solutionContent = TextWithMediaAttachments(content),
        telegramMessageInfo = TelegramMessageInfo(student.tgId, defaultMessageId),
        timestamp = defaultTimestamp,
      )
    val solutionId = apis.studentApi.inputSolution(solutionInputRequest).value
    return Solution(
      solutionId,
      solutionInputRequest.studentId,
      solutionInputRequest.telegramMessageInfo.chatId,
      solutionInputRequest.telegramMessageInfo.messageId,
      solutionInputRequest.problemId,
      solutionInputRequest.solutionContent,
      null,
      solutionInputRequest.timestamp,
    )
  }

  fun assessment(teacher: Teacher, solution: Solution, grade: Int): SolutionAssessment {
    val assessment = SolutionAssessment(grade, TextWithMediaAttachments())
    apis.teacherApi.assessSolution(
      solutionId = solution.id,
      teacherId = teacher.id,
      solutionAssessment = assessment,
      timestamp = defaultTimestamp,
    )
    return assessment
  }

  fun movingDeadlinesRequest(student: Student, newDeadline: LocalDateTime) {
    apis.studentApi.requestReschedulingDeadlines(student.id, newDeadline)
  }

  fun moveDeadlines(student: Student, newDeadline: LocalDateTime) {
    apis.adminApi.moveAllDeadlinesForStudent(student.id, newDeadline)
  }
}

fun buildData(apis: ApiCollection, block: TestDataBuilder.() -> Unit) =
  TestDataBuilder(apis).apply(block)
