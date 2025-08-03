package com.github.heheteam.commonlib.logic

import com.github.heheteam.commonlib.Challenge
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.AssignmentId
import com.github.heheteam.commonlib.interfaces.ChallengeId
import com.github.heheteam.commonlib.interfaces.ChallengeStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding

class ChallengeService(private val challengeStorage: ChallengeStorage) {
  fun getActiveChallengingProblems(
    studentId: StudentId,
    courseId: CourseId,
  ): Result<Map<Challenge, List<Problem>>, EduPlatformError> = binding {
    challengeStorage.getProblemsWithChallengesFromCourseForStudent(courseId, studentId).bind()
  }

  fun createChallenge(
    courseId: CourseId,
    assignmentId: AssignmentId,
    challengeDescription: String,
    challengingProblemsDescriptions: List<ProblemDescription>,
  ): Result<ChallengeId?, EduPlatformError> = binding {
    challengeStorage
      .createChallenge(
        courseId,
        assignmentId,
        challengeDescription,
        challengingProblemsDescriptions,
      )
      .bind()
  }
}
