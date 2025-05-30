package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.SubmissionDistributor
import com.github.heheteam.commonlib.interfaces.SubmissionId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.SubmissionStatusMessageInfo
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding

@Suppress("LongParameterList") // will go away with refactoring after tests are there
class TelegramMessagesJournalUpdater
internal constructor(
  private val gradeTable: GradeTable,
  private val submissionDistributor: SubmissionDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val teacherBotTelegramController: TeacherBotTelegramController,
) : JournalUpdater {
  override suspend fun updateJournalDisplaysForSubmission(submissionId: SubmissionId) {
    coroutineBinding {
      val submissionStatusMessageInfo = extractSubmissionStatusMessageInfo(submissionId).bind()
      val groupTechnicalMessage = technicalMessageStorage.resolveGroupMessage(submissionId).bind()
      teacherBotTelegramController.updateSubmissionStatusMessageInCourseGroupChat(
        groupTechnicalMessage,
        submissionStatusMessageInfo,
      )
    }
    coroutineBinding {
      val submissionStatusMessageInfo = extractSubmissionStatusMessageInfo(submissionId).bind()
      val personalTechnicalMessage =
        technicalMessageStorage.resolvePersonalMessage(submissionId).bind()
      teacherBotTelegramController.updateSubmissionStatusMessageDM(
        personalTechnicalMessage,
        submissionStatusMessageInfo,
      )
    }
  }

  private fun extractSubmissionStatusMessageInfo(
    submissionId: SubmissionId
  ): Result<SubmissionStatusMessageInfo, ResolveError<out Any>> {
    return binding {
      val gradingEntries = gradeTable.getGradingsForSubmission(submissionId)
      val submission = submissionDistributor.resolveSubmission(submissionId).bind()
      val problem = problemStorage.resolveProblem(submission.problemId).bind()
      val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
      val student = studentStorage.resolveStudent(submission.studentId).bind()
      val responsibleTeacher =
        submission.responsibleTeacherId?.let { teacherStorage.resolveTeacher(it).bind() }
      SubmissionStatusMessageInfo(
        submissionId,
        assignment.description,
        problem.number,
        student,
        responsibleTeacher,
        gradingEntries,
      )
    }
  }
}
