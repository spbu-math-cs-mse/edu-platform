package com.github.heheteam.commonlib.database

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.ResponsibleTeacherResolver
import com.github.heheteam.commonlib.api.TeacherId
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr

class FirstTeacherResolver(
  val problemStorage: ProblemStorage,
  val assignmentStorage: AssignmentStorage,
  val coursesDistributor: CoursesDistributor,
) : ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(problemId: ProblemId): Result<TeacherId, String> {
    val result =
      binding {
          val problem = problemStorage.resolveProblem(problemId).bind()
          val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
          val teachers = coursesDistributor.getTeachers(assignment.courseId).sortedBy { it.id.id }
          teachers.firstOrNull()?.id.toResultOr { "No teachers" }.bind()
        }
        .mapError { it.toString() }
    return result
  }
}

class RandomTeacherResolver(
  val problemStorage: ProblemStorage,
  val assignmentStorage: AssignmentStorage,
  val coursesDistributor: CoursesDistributor,
) : ResponsibleTeacherResolver {
  override fun resolveResponsibleTeacher(problemId: ProblemId): Result<TeacherId, String> {
    val result =
      binding {
        val problem = problemStorage.resolveProblem(problemId).bind()
        val assignment = assignmentStorage.resolveAssignment(problem.assignmentId).bind()
        val teachers = coursesDistributor.getTeachers(assignment.courseId).shuffled()
        println(teachers)
        teachers.firstOrNull()?.id.toResultOr { "No teachers" }.bind()
      }
        .mapError { it.toString() }
    return result
  }
}