package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.SubmissionAssessment
import com.github.heheteam.commonlib.errors.EduPlatformError
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.telegram.StudentBotTelegramController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding

internal class StudentNewGradeNotifierImpl(
  private val studentBotTelegramController: StudentBotTelegramController,
  private val problemStorage: ProblemStorage,
  private val submissionDistributor: SubmissionDistributor,
) : StudentNewGradeNotifier {
  override suspend fun notifyStudentOnNewAssessment(
    submissionId: SubmissionId,
    assessment: SubmissionAssessment,
  ): Result<Unit, EduPlatformError> = coroutineBinding {
    val submission = submissionDistributor.resolveSubmission(submissionId).bind()
    val problem = problemStorage.resolveProblem(submission.problemId).bind()

    studentBotTelegramController.notifyStudentOnNewAssessment(
      submission.chatId,
      submission.messageId,
      submission.studentId,
      problem,
      assessment,
    )
  }
}
