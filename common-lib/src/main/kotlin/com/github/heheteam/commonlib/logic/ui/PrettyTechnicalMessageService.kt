package com.github.heheteam.commonlib.logic.ui

import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.GradeTable
import com.github.heheteam.commonlib.interfaces.GradingEntry
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.SolutionDistributor
import com.github.heheteam.commonlib.interfaces.SolutionId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth

class PrettyTechnicalMessageService
internal constructor(
  private val solutionDistributor: SolutionDistributor,
  private val problemStorage: ProblemStorage,
  private val assignmentStorage: AssignmentStorage,
  private val studentStorage: StudentStorage,
  private val gradeTable: GradeTable,
  private val teacherStorage: TeacherStorage,
) {
  fun createPrettyDisplayForTechnicalForTechnicalMessage(solutionId: SolutionId) =
    binding {
        val gradingEntries = gradeTable.getGradingsForSolution(solutionId)
        val solution = solutionDistributor.resolveSolution(solutionId).bind()
        val problem = problemStorage.resolveProblem(solution.problemId).bind()
        val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
        val student = studentStorage.resolveStudent(solution.studentId).bind()
        val responsibleTeacher =
          solution.responsibleTeacherId?.let { teacherStorage.resolveTeacher(it).get() }
        buildString {
          appendLine("(Ответьте на это сообщение или нажмите на кнопки внизу для проверки)")
          appendLine("Отправка #${solutionId.id}")
          appendLine("Задача ${assignment.description}:${problem.number}")
          appendLine("Решение отправил ${student.name} ${student.surname} (id=${student.id})")
          if (responsibleTeacher != null)
            appendLine(
              "Проверяющий: ${responsibleTeacher.name} ${responsibleTeacher.surname} (id=${responsibleTeacher.id})"
            )
          appendLine()

          gradingEntries.forEach { entry: GradingEntry ->
            teacherStorage.resolveTeacher(entry.teacherId).map { teacher ->
              appendLine("Проверил ${teacher.name} ${teacher.surname} (id=${teacher.id}")
              appendLine("Дата: ${entry.timestamp}")
              appendLine("Оценка: ${entry.assessment.grade}")
              appendLine("Комментарий: \"${entry.assessment.comment}\"")
              appendLine("---")
            }
          }
        }
      }
      .mapBoth(success = { it }, failure = { it.toString() })
}
