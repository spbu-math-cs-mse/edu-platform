package com.github.heheteam.teacherbot.logic

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.GradeTable
import com.github.heheteam.commonlib.api.GradingEntry
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.SolutionDistributor
import com.github.heheteam.commonlib.api.SolutionId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.get
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PrettyTechnicalMessageService: KoinComponent {
  private val solutionDistributor: SolutionDistributor by inject()
  private val problemStorage: ProblemStorage by inject()
  private val assignmentStorage: AssignmentStorage by inject()
  private val studentStorage: StudentStorage by inject()
  private val gradeTable: GradeTable by inject()
  private val teacherStorage: TeacherStorage by inject()

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
