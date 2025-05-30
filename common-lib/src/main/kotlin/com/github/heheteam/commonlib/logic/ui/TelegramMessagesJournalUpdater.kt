package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.SolutionStatusMessageInfo
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get

@Suppress("LongParameterList") // will go away with refactoring after tests are there
class TelegramMessagesJournalUpdater
internal constructor(
  private val gradeTable: GradeTable,
  private val solutionDistributor: SolutionDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val studentStorage: StudentStorage,
  private val teacherStorage: TeacherStorage,
  private val technicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val teacherBotTelegramController: TeacherBotTelegramController,
) : JournalUpdater {
  override suspend fun updateJournalDisplaysForSolution(solutionId: SolutionId) {
    coroutineBinding {
      val solutionStatusMessageInfo = extractSolutionStatusMessageInfo(solutionId).bind()
      val groupTechnicalMessage = technicalMessageStorage.resolveGroupMessage(solutionId).bind()
      teacherBotTelegramController.updateSolutionStatusMessageInCourseGroupChat(
        groupTechnicalMessage,
        solutionStatusMessageInfo,
      )
    }
    coroutineBinding {
      val solutionStatusMessageInfo = extractSolutionStatusMessageInfo(solutionId).bind()
      val personalTechnicalMessage =
        technicalMessageStorage.resolvePersonalMessage(solutionId).bind()
      teacherBotTelegramController.updateSolutionStatusMessageDM(
        personalTechnicalMessage,
        solutionStatusMessageInfo,
      )
    }
  }

  private fun extractSolutionStatusMessageInfo(
    solutionId: SolutionId
  ): Result<SolutionStatusMessageInfo, ResolveError<out Any>> {
    return binding {
      val gradingEntries = gradeTable.getGradingsForSolution(solutionId)
      val solution = solutionDistributor.resolveSolution(solutionId).bind()
      val problem = problemStorage.resolveProblem(solution.problemId).bind()
      val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
      val student = studentStorage.resolveStudent(solution.studentId).bind()
      val responsibleTeacher =
        solution.responsibleTeacherId?.let { teacherStorage.resolveTeacher(it).bind() }
      SolutionStatusMessageInfo(
        solutionId,
        assignment.description,
        problem.number,
        student,
        responsibleTeacher,
        gradingEntries,
      )
    }
  }
}
