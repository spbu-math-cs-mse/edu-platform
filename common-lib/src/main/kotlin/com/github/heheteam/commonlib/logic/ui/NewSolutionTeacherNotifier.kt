package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.ResolveError
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.TelegramMessageInfo
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CoursesDistributor
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.heheteam.commonlib.interfaces.TelegramTechnicalMessagesStorage
import com.github.heheteam.commonlib.telegram.SolutionStatusMessageInfo
import com.github.heheteam.commonlib.telegram.TeacherBotTelegramController
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException

@Suppress("LongParameterList") // fix after test exist
internal class NewSolutionTeacherNotifier(
  private val telegramTechnicalMessageStorage: TelegramTechnicalMessagesStorage,
  private val solutionCourseResolver: SolutionCourseResolver,
  private val teacherBotTelegramController: TeacherBotTelegramController,
  private val solutionDistributor: SolutionDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val teacherStorage: TeacherStorage,
  private val coursesDistributor: CoursesDistributor,
) {
  suspend fun notifyNewSolution(solution: Solution): Result<Unit, SolutionSendingError> =
    coroutineBinding {
      sendSolutionToTeacherPersonally(solution)
      sendSolutionToGroup(solution)
      val teacherId = solution.responsibleTeacherId
      if (teacherId != null) {
        updateMenuMessageInPersonalMessages(teacherId, solution)
      }
      val courseId = solutionCourseResolver.resolveCourse(solution.id).get()
      if (courseId != null) {
        updateMenuMessageInGroup(courseId, solution)
      }
    }

  private suspend fun updateMenuMessageInGroup(courseId: CourseId, solution: Solution) {
    coroutineBinding {
      val menuMessages =
        telegramTechnicalMessageStorage
          .resolveGroupMenuMessage(courseId)
          .mapError { FailedToResolveSolution(solution) }
          .bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage
          .resolveGroupFirstUncheckedSolutionMessage(courseId)
          .mapError { FailedToResolveSolution(solution) }
          .bind()
      teacherBotTelegramController.sendMenuMessage(
        chatId,
        messageId?.let { TelegramMessageInfo(chatId, it) },
      )
    }
  }

  private suspend fun updateMenuMessageInPersonalMessages(
    teacherId: TeacherId,
    solution: Solution,
  ) {
    coroutineBinding {
      val menuMessages =
        telegramTechnicalMessageStorage
          .resolveTeacherMenuMessage(teacherId)
          .mapError { FailedToResolveSolution(solution) }
          .bind()
      deleteMenuMessages(menuMessages)

      val (chatId, messageId) =
        telegramTechnicalMessageStorage
          .resolveTeacherFirstUncheckedSolutionMessage(teacherId)
          .mapError { FailedToResolveSolution(solution) }
          .bind()
      val menuMessage =
        teacherBotTelegramController
          .sendMenuMessage(chatId, messageId?.let { TelegramMessageInfo(chatId, it) })
          .mapError { FailedToResolveSolution(solution) }
          .bind()

      telegramTechnicalMessageStorage.updateTeacherMenuMessage(
        TelegramMessageInfo(menuMessage.chatId, menuMessage.messageId)
      )
    }
  }

  private suspend fun sendSolutionToGroup(solution: Solution): Result<Unit, SolutionSendingError> =
    coroutineBinding {
      val problem =
        problemStorage
          .resolveProblem(solution.problemId)
          .mapError { FailedToResolveSolution(solution) }
          .bind()
      val assignment =
        assignmentStorage
          .resolveAssignment(problem.assignmentId)
          .mapError { FailedToResolveSolution(solution) }
          .bind()

      val chat =
        coursesDistributor
          .resolveCourseGroup(assignment.courseId)
          .mapError { FailedToResolveSolution(solution) }
          .bind()
      if (chat == null) {
        Err(SendToGroupSolutionError(assignment.courseId)).bind<Nothing>()
      }
      val solutionStatusInfo =
        extractSolutionStatusMessageInfo(solution.id)
          .mapError { FailedToResolveSolution(solution) }
          .bind()
      val groupMessage =
        teacherBotTelegramController
          .sendInitSolutionStatusMessageInCourseGroupChat(chat, solutionStatusInfo)
          .mapError { SendToGroupSolutionError(assignment.courseId) }
          .bind()
      telegramTechnicalMessageStorage.registerGroupSolutionPublication(solution.id, groupMessage)
    }

  private suspend fun sendSolutionToTeacherPersonally(
    solution: Solution
  ): Result<Unit, SolutionSendingError> = coroutineBinding {
    val teacherId =
      solution.responsibleTeacherId.toResultOr { NoResponsibleTeacherFor(solution) }.bind()
    val teacher =
      teacherStorage.resolveTeacher(teacherId).mapError { NoResponsibleTeacherFor(solution) }.bind()
    val solutionStatusInfo =
      extractSolutionStatusMessageInfo(solution.id)
        .mapError { FailedToResolveSolution(solution) }
        .bind()
    val personalTechnicalMessage =
      teacherBotTelegramController
        .sendInitSolutionStatusMessageDM(teacher.tgId, solutionStatusInfo)
        .mapError { SendToTeacherSolutionError(teacherId) }
        .bind()
    telegramTechnicalMessageStorage.registerPersonalSolutionPublication(
      solution.id,
      personalTechnicalMessage,
    )
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
        solution.responsibleTeacherId?.let { teacherStorage.resolveTeacher(it).get() }
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

  private suspend fun deleteMenuMessages(menuMessages: List<TelegramMessageInfo>) {
    menuMessages.map { menuMessage ->
      try {
        teacherBotTelegramController.deleteMessage(menuMessage)
      } catch (e: CommonRequestException) {
        KSLog.warning("Menu message has already been deleted:\n$e")
      }
    }
  }
}
