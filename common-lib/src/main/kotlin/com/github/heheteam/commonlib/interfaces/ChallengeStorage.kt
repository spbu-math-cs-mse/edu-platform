package com.github.heheteam.commonlib.interfaces

import com.github.heheteam.commonlib.Challenge
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.errors.DatabaseExceptionError
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.michaelbull.result.Result

interface ChallengeStorage {
  fun createChallenge(
    courseId: CourseId,
    assignmentId: AssignmentId,
    challengeDescription: String,
    challengingProblemsDescriptions: List<ProblemDescription>,
  ): Result<ChallengeId?, DatabaseExceptionError>

  fun createProblem(
    challengeId: ChallengeId,
    serialNumber: Int,
    problemDescription: ProblemDescription,
  ): Result<ChallengingProblemId, EduPlatformError>

  fun getProblemsWithChallengesFromCourseForStudent(
    courseId: CourseId,
    studentId: StudentId,
  ): Result<Map<Challenge, List<Problem>>, EduPlatformError>
}
