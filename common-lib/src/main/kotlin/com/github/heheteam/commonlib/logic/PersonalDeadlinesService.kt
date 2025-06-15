package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.PersonalDeadlineStorage
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.notifications.BotEventBus
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import kotlinx.datetime.LocalDateTime

internal class PersonalDeadlinesService(
  private val studentStorage: StudentStorage,
  private val problemStorage: ProblemStorage,
  private val personalDeadlineStorage: PersonalDeadlineStorage,
  private val botEventBus: BotEventBus,
) {
  suspend fun requestReschedulingDeadlines(studentId: StudentId, newDeadline: LocalDateTime) {
    botEventBus.publishNewDeadlineRequest(studentId, newDeadline)
  }

  /**
   * Moves all deadlines to the newDeadline. The new deadline calculates as `max(new deadline,
   * original deadline)`
   *
   * @param studentId
   */
  suspend fun moveDeadlinesForStudent(studentId: StudentId, newDeadline: LocalDateTime) {
    personalDeadlineStorage.updateDeadlineForStudent(studentId, newDeadline)
    val student = studentStorage.resolveStudent(studentId).value
    botEventBus.publishMovingDeadlineEvent(student.tgId, newDeadline)
  }

  fun calculateNewDeadlines(studentId: StudentId, problems: List<Problem>): List<Problem> {
    val newDeadline = personalDeadlineStorage.resolveDeadline(studentId)
    return problems.map { problem ->
      problem.copy(deadline = calculateDeadline(problem.deadline, newDeadline))
    }
  }

  fun getActiveProblems(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Map<Assignment, List<Problem>>, EduPlatformError> = binding {
    val problems = problemStorage.getProblemsWithAssignmentsFromCourse(courseId).bind()
    val newDeadline = personalDeadlineStorage.resolveDeadline(studentId)
    problems
      .map { (assignment, problems) ->
        assignment to
          problems.map { it.copy(deadline = calculateDeadline(it.deadline, newDeadline)) }
      }
      .toMap()
  }

  private fun calculateDeadline(
    deadline: LocalDateTime?,
    newDeadline: LocalDateTime?,
  ): LocalDateTime? =
    if (deadline == null) {
      null
    } else if (newDeadline == null) {
      deadline
    } else if (deadline > newDeadline) {
      deadline
    } else {
      newDeadline
    }
}
