package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.SubmissionId

interface JournalUpdater {
  suspend fun updateJournalDisplaysForSubmission(submissionId: SubmissionId)
}
