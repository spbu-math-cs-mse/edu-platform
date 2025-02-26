package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.michaelbull.result.Result
import kotlinx.datetime.LocalDateTime

interface ProblemStorage {
  fun resolveProblem(problemId: ProblemId): Result<Problem, ResolveError<ProblemId>>

  fun createProblem(
    assignmentId: AssignmentId,
    serialNumber: Int,
    number: String,
    maxScore: Grade,
    description: String,
    deadline: LocalDateTime? = null,
  ): ProblemId

  fun getProblemsFromAssignment(assignmentId: AssignmentId): List<Problem>

  fun getProblemsFromCourse(courseId: CourseId): List<Problem>

  fun getProblemsWithAssignmentsFromCourse(courseId: CourseId): Map<Assignment, List<Problem>>
}
