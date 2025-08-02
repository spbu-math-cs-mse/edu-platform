package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import kotlinx.datetime.LocalDateTime

fun Map<Assignment, List<Problem>>.filterByDeadlineAndSort(
  currentMoscowTime: LocalDateTime
): Iterable<Pair<Assignment, List<Problem>>> =
  this.map { (assignment, problems) ->
      assignment to
        problems.filter { !isDeadlineMissed(it, currentMoscowTime) }.sortedBy { it.serialNumber }
    }
    .filter { (_, problems) -> problems.isNotEmpty() }
    .sortedBy { (assignment, _) -> assignment.serialNumber }

fun isDeadlineMissed(problem: Problem, currentMoscowTime: kotlinx.datetime.LocalDateTime): Boolean {
  val problemDeadline = problem.deadline
  return problemDeadline != null && problemDeadline < currentMoscowTime
}
