package com.github.heheteam.commonlib.api

interface RatingRecorder {
  fun updateRating(courseId: CourseId)

  fun updateRating(problemId: ProblemId)

  fun updateRating(solutionId: SolutionId)
}
