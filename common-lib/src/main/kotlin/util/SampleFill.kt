package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage

fun generateCourse(
  name: String,
  coursesDistributor: CoursesDistributor,
  assignmentStorage: AssignmentStorage,
  problemStorage: ProblemStorage,
  assignmentsPerCourse: Int = 1,
  problemsPerAssignment: Int = 4,
): CourseId {
  val courseId = coursesDistributor.createCourse(name)
  (0..assignmentsPerCourse).map { assgnNum ->
    assignmentStorage.createAssignment(
      courseId,
      "assignment $courseId.$assgnNum",
      (0..problemsPerAssignment).map { ("p$courseId.$assgnNum.$it") },
      problemStorage,
    )
  }
  return courseId
}

fun fillWithSamples(
  coursesDistributor: CoursesDistributor,
  problemStorage: ProblemStorage,
  assignmentStorage: AssignmentStorage,
) {
  val realAnalysis = generateCourse("Начала мат. анализа", coursesDistributor, assignmentStorage, problemStorage)
  val probTheory = generateCourse("Теория вероятностей", coursesDistributor, assignmentStorage, problemStorage)
  val linAlgebra = generateCourse("Линейная алгебра", coursesDistributor, assignmentStorage, problemStorage)
  val complAnalysis = generateCourse("ТФКП", coursesDistributor, assignmentStorage, problemStorage)

  (0 until 5).map { studentId -> coursesDistributor.addRecord(studentId.toLong(), realAnalysis) }
  (0 until 5).map { studentId -> coursesDistributor.addRecord(studentId.toLong(), probTheory) }
  (5 until 10).map { studentId -> coursesDistributor.addRecord(studentId.toLong(), probTheory) }
  (5 until 10).map { studentId -> coursesDistributor.addRecord(studentId.toLong(), linAlgebra) }
}
