package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Grade
import com.github.heheteam.commonlib.Problem
import com.github.michaelbull.result.Result

interface ProblemStorage {
  fun resolveProblem(problemId: ProblemId): Result<Problem, ResolveError<ProblemId>>

  fun createProblem(
    assignmentId: AssignmentId,
    number: String,
    maxScore: Grade,
    description: String,
  ): ProblemId

  fun getProblemsFromAssignment(assignmentId: AssignmentId): List<Problem>

  fun getProblemsFromCourse(courseId: CourseId): List<Problem>
}
