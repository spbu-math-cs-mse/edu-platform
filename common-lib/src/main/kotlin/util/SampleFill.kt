package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.api.*

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
      "assignment ${courseId.id}.$assgnNum",
      (0..problemsPerAssignment).map { ("p${courseId.id}.$assgnNum.$it") },
      problemStorage,
    )
  }
  return courseId
}

fun fillWithSamples(
  coursesDistributor: CoursesDistributor,
  problemStorage: ProblemStorage,
  assignmentStorage: AssignmentStorage,
  studentStorage: StudentStorage,
  teacherStorage: TeacherStorage,
): List<CourseId> {
  val realAnalysis =
    generateCourse(
      "Начала мат. анализа",
      coursesDistributor,
      assignmentStorage,
      problemStorage,
    )
  val probTheory =
    generateCourse(
      "Теория вероятностей",
      coursesDistributor,
      assignmentStorage,
      problemStorage,
    )
  val linAlgebra =
    generateCourse(
      "Линейная алгебра",
      coursesDistributor,
      assignmentStorage,
      problemStorage,
    )
  val complAnalysis =
    generateCourse(
      "ТФКП",
      coursesDistributor,
      assignmentStorage,
      problemStorage,
    )
  val students = (0..10).map { studentStorage.createStudent() }
  students.slice(0..<5).map { studentId ->
    coursesDistributor.addStudentToCourse(
      studentId,
      realAnalysis,
    )
  }
  students.slice(0..<5).map { studentId ->
    coursesDistributor.addStudentToCourse(
      studentId,
      probTheory,
    )
  }
  students.slice(5..<10).map { studentId ->
    coursesDistributor.addStudentToCourse(
      studentId,
      probTheory,
    )
  }
  students.slice(5..<10).map { studentId ->
    coursesDistributor.addStudentToCourse(
      studentId,
      linAlgebra,
    )
  }
  (0..10).map { teacherStorage.createTeacher() }
  println("first student is ${students.first()}")
  return listOf(realAnalysis, probTheory, linAlgebra, complAnalysis)
}
