package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentId
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.reset
import org.jetbrains.exposed.sql.Database

fun generateCourse(
  name: String,
  coursesDistributor: CoursesDistributor,
  assignmentStorage: AssignmentStorage,
  problemStorage: ProblemStorage,
  assignmentsPerCourse: Int = 2,
  problemsPerAssignment: Int = 5,
): CourseId {
  val courseId = coursesDistributor.createCourse(name)
  (1..assignmentsPerCourse).map { assgnNum ->
    assignmentStorage.createAssignment(
      courseId,
      "assignment $courseId.$assgnNum",
      (1..problemsPerAssignment).map { ProblemDescription(it, "$assgnNum.$it", "", 1, null) },
      problemStorage,
    )
  }
  return courseId
}

data class FillContent(
  val courses: List<CourseId>,
  val students: List<StudentId>,
  val teachers: List<TeacherId>,
)

@Suppress("LongParameterList")
fun fillWithSamples(
  coursesDistributor: CoursesDistributor,
  problemStorage: ProblemStorage,
  assignmentStorage: AssignmentStorage,
  studentStorage: StudentStorage,
  teacherStorage: TeacherStorage,
  database: Database,
): FillContent {
  reset(database)
  val realAnalysis =
    generateCourse("Начала мат. анализа", coursesDistributor, assignmentStorage, problemStorage)
  val probTheory =
    generateCourse("Теория вероятностей", coursesDistributor, assignmentStorage, problemStorage)
  val linAlgebra =
    generateCourse("Линейная алгебра", coursesDistributor, assignmentStorage, problemStorage)
  val complAnalysis = generateCourse("ТФКП", coursesDistributor, assignmentStorage, problemStorage)
  val students = createStudent(studentStorage)
  students.slice(0..<5).map { studentId ->
    coursesDistributor.addStudentToCourse(studentId, realAnalysis)
    coursesDistributor.addStudentToCourse(studentId, probTheory)
  }
  students.slice(5..<10).map { studentId ->
    coursesDistributor.addStudentToCourse(studentId, probTheory)
    coursesDistributor.addStudentToCourse(studentId, linAlgebra)
  }
  val teachers =
    listOf("Павел" to "Мозоляко", "Егор" to "Тихонов").map {
      teacherStorage.createTeacher(it.first, it.second)
    }

  coursesDistributor.addTeacherToCourse(TeacherId(1), realAnalysis)

  return FillContent(
    courses = listOf(realAnalysis, probTheory, linAlgebra, complAnalysis),
    students = students,
    teachers = teachers,
  )
}

private fun createStudent(studentStorage: StudentStorage): List<StudentId> {
  val students =
    listOf(
        "Алексей" to "Иванов",
        "Мария" to "Петрова",
        "Дмитрий" to "Сидоров",
        "Анна" to "Смирнова",
        "Иван" to "Кузнецов",
        "Елена" to "Попова",
        "Андрей" to "Семенов",
        "Ольга" to "Соколова",
        "Андрей" to "Михайлов",
        "Николай" to "Васильев",
      )
      .map { studentStorage.createStudent(it.first, it.second) }
  return students
}
