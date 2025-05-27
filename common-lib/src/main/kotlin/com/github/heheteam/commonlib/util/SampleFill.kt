package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.AdminStorage
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CourseStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Database

internal fun generateCourse(
  name: String,
  courseStorage: CourseStorage,
  assignmentStorage: AssignmentStorage,
  assignmentsPerCourse: Int = 2,
  problemsPerAssignment: Int = 5,
): CourseId {
  val courseId = courseStorage.createCourse(name)
  (1..assignmentsPerCourse).map { assignNum ->
    assignmentStorage.createAssignment(
      courseId,
      "assignment $courseId.$assignNum",
      (0..<problemsPerAssignment).map {
        val timeZone = TimeZone.currentSystemDefault()
        val deadline =
          if (it % 2 == 0) (Clock.System.now() + 2.minutes).toLocalDateTime(timeZone) else null
        val number = "${it / 2 + 1}" + ('a' + it % 2)
        ProblemDescription(it, number, "", 1, deadline)
      },
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
internal fun fillWithSamples(
  courseStorage: CourseStorage,
  assignmentStorage: AssignmentStorage,
  adminStorage: AdminStorage,
  studentStorage: StudentStorage,
  teacherStorage: TeacherStorage,
  database: Database,
): FillContent {
  reset(database)
  // Admins
  listOf("Кабан" to "Кабаныч").map { adminStorage.createAdmin(it.first, it.second) }

  val realAnalysis = generateCourse("Начала мат. анализа", courseStorage, assignmentStorage)
  val probTheory = generateCourse("Теория вероятностей", courseStorage, assignmentStorage)
  val linAlgebra = generateCourse("Линейная алгебра", courseStorage, assignmentStorage)
  val complAnalysis = generateCourse("ТФКП", courseStorage, assignmentStorage)
  val students = createStudent(studentStorage)
  students.slice(0..<5).map { studentId ->
    courseStorage.addStudentToCourse(studentId, realAnalysis)
    courseStorage.addStudentToCourse(studentId, probTheory)
  }
  students.slice(5..<10).map { studentId ->
    courseStorage.addStudentToCourse(studentId, probTheory)
    courseStorage.addStudentToCourse(studentId, linAlgebra)
  }
  val teachers =
    listOf("Павел" to "Мозоляко", "Егор" to "Тихонов").map {
      teacherStorage.createTeacher(it.first, it.second)
    }

  courseStorage.addTeacherToCourse(TeacherId(1), realAnalysis)
  courseStorage.addTeacherToCourse(TeacherId(2), realAnalysis)

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
