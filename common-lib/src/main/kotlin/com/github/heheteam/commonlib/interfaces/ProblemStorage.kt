package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.errors.ResolveError
import com.github.michaelbull.result.Result

interface ProblemStorage {
  fun resolveProblem(problemId: ProblemId): Result<Problem, ResolveError<ProblemId>>

  fun createProblem(
    assignmentId: AssignmentId,
    serialNumber: Int,
    problemDescription: ProblemDescription,
  ): Result<ProblemId, EduPlatformError>

  fun getProblemsFromAssignment(assignmentId: AssignmentId): Result<List<Problem>, EduPlatformError>

  fun getProblemsFromCourse(courseId: CourseId): Result<List<Problem>, EduPlatformError>

  fun getProblemsWithAssignmentsFromCourse(
    courseId: CourseId
  ): Result<Map<Assignment, List<Problem>>, EduPlatformError>
}
