package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.*
import com.github.heheteam.commonlib.api.*
import com.github.michaelbull.result.Result
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import java.time.LocalDateTime

// this class represents a service given by the bot;
// students ids are parameters in this class
class StudentCore(
  private val solutionDistributor: SolutionDistributor,
  private val coursesDistributor: CoursesDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val gradeTable: GradeTable,
  private val notificationService: NotificationService,
  private val botEventBus: BotEventBus,
  private val messagesDistributor: ScheduledMessagesDistributor,
  private val studentStorage: StudentStorage,
) {
  init {
    botEventBus.subscribeToGradeEvents { studentId, chatId, messageId, assessment, problem ->
      notifyAboutGrade(studentId, chatId, messageId, assessment, problem)
    }
  }

  fun updateTgId(
    studentId: StudentId,
    tgId: UserId,
  ): Result<Unit, ResolveError<StudentId>> {
    println("updateTgId $studentId $tgId")
    return studentStorage.updateTgId(studentId, tgId)
  }

  // returns list of chatId to message content
  fun sendMessagesIfExistUnsent(date: LocalDateTime): List<Pair<Long, String>> {
    val messagesToSent = messagesDistributor.getUnsentMessagesUpToDate(date)
    val allMessages = messagesToSent.flatMap { message ->
      val students = coursesDistributor.getStudents(message.courseId)
      println(students)
      students.map { it.tgChatId to message.message }
    }
    messagesDistributor.markMessagesUpToDateAsSent(date)
    return allMessages
  }

  fun tmpSendSampleMessage(courseId: CourseId, date: LocalDateTime) {
    messagesDistributor.addMessage(
      ScheduledMessage(
        courseId,
        date,
        "Обратите пожалуйста внимание на обновления по расписанию",
      ),
    )
    println("Scheduled message for course id=$courseId on time $date")
    println("On this course: ${coursesDistributor.getStudents(courseId)}")
  }

  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, Grade?>> {
    val grades =
      gradeTable
        .getStudentPerformance(studentId, assignmentId, solutionDistributor)
    val gradedProblems =
      problemStorage
        .getProblemsFromAssignment(assignmentId)
        .sortedBy { problem -> problem.number }
        .map { problem -> problem to grades[problem.id] }
    return gradedProblems
  }

  fun getStudentCourses(studentId: StudentId): List<Course> =
    coursesDistributor
      .getStudentCourses(studentId)

  fun getCourseAssignments(courseId: CourseId): List<Assignment> =
    assignmentStorage.getAssignmentsForCourse(courseId)

  fun addRecord(
    studentId: StudentId,
    courseId: CourseId,
  ) = coursesDistributor.addStudentToCourse(studentId, courseId)

  fun getCourses(): List<Course> = coursesDistributor.getCourses()

  fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
  ) {
    solutionDistributor.inputSolution(
      studentId,
      chatId,
      messageId,
      solutionContent,
      problemId,
    )
  }

  fun getCoursesBulletList(studentId: StudentId): String {
    val studentCourses = coursesDistributor.getStudentCourses(studentId)
    val notRegisteredMessage = "Вы не записаны ни на один курс!"
    return if (studentCourses.isNotEmpty()) {
      studentCourses
        .joinToString("\n") { course ->
          "- " + course.name
        }
    } else {
      notRegisteredMessage
    }
  }

  fun getProblemsFromAssignment(assignment: Assignment): List<Problem> =
    problemStorage.getProblemsFromAssignment(assignment.id)

  suspend fun notifyAboutGrade(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) {
    notificationService.notifyStudentAboutGrade(
      studentId,
      chatId,
      messageId,
      assessment,
      problem,
    )
  }
}
