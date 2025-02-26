package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import java.time.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

fun Map<Assignment, List<Problem>>.filterByDeadlineAndSort():
  Iterable<Pair<Assignment, List<Problem>>> =
  this.map { (assignment, problems) ->
      assignment to problems.filter { !isDeadlineMissed(it) }.sortedBy { it.serialNumber }
    }
    .filter { (_, problems) -> problems.isNotEmpty() }
    .sortedBy { (assignment, _) -> assignment.serialNumber }

fun isDeadlineMissed(problem: Problem): Boolean {
  val problemDeadline = problem.deadline
  return problemDeadline != null && LocalDateTime.now().toKotlinLocalDateTime() > problemDeadline
}
