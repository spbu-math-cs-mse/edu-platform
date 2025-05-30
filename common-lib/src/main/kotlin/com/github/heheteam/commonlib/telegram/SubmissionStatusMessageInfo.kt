package com.github.heheteam.commonlib.telegram

import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.Teacher
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.SubmissionId

data class SubmissionStatusMessageInfo(
  val submissionId: SubmissionId,
  val assignmentDisplayName: String,
  val problemDisplayName: String,
  val student: Student,
  val responsibleTeacher: Teacher?,
  val gradingEntries: List<GradingEntry>,
)
