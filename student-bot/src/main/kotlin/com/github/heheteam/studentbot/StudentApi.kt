package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionInputRequest
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.api.AssignmentId
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemGrade
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.logic.AcademicWorkflowService
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

// this class represents a service given by the bot;
// students ids are parameters in this class
class StudentApi(
  private val coursesDistributor: CoursesDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val academicWorkflowService: AcademicWorkflowService,
) {
  fun getGradingForAssignment(
    assignmentId: AssignmentId,
    studentId: StudentId,
  ): List<Pair<Problem, ProblemGrade>> {
    return academicWorkflowService.getGradingsForAssignment(assignmentId, studentId)
  }

  fun getStudentCourses(studentId: StudentId): List<Course> =
    coursesDistributor.getStudentCourses(studentId)

  fun getCourseAssignments(courseId: CourseId): List<Assignment> =
    assignmentStorage.getAssignmentsForCourse(courseId)

  fun applyForCourse(studentId: StudentId, courseId: CourseId) =
    coursesDistributor.addStudentToCourse(studentId, courseId)

  fun getCourses(): List<Course> = coursesDistributor.getCourses()

  fun inputSolution(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    solutionContent: SolutionContent,
    problemId: ProblemId,
  ) {
    academicWorkflowService.sendSolution(
      SolutionInputRequest(
        studentId,
        problemId,
        solutionContent,
        TelegramMessageInfo(chatId, messageId),
        LocalDateTime.now().toKotlinLocalDateTime(),
      )
    )
  }

  fun getProblemsFromAssignment(assignment: Assignment): List<Problem> =
    problemStorage.getProblemsFromAssignment(assignment.id)
}
