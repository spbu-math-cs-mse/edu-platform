package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.EduPlatformError
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.michaelbull.result.Result

interface JournalUpdater {
  suspend fun updateJournalDisplaysForSubmission(
    submissionId: SubmissionId
  ): Result<Unit, EduPlatformError>
}
