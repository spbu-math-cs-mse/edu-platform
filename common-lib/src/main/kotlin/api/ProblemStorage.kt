package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Problem

interface ProblemStorage {
  fun resolveProblem(id: ProblemId): Problem
  fun createProblem(assignmentId: AssignmentId, number: String): ProblemId
}
