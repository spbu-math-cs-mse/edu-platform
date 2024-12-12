package com.github.heheteam.commonlib.googlesheets

import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.RatingRecorder

class MockRatingRecorder : RatingRecorder {
  override fun updateRating(courseId: CourseId) {
  }

  override fun updateRating(problemId: ProblemId) {
  }
}
