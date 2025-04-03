package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.database.reset
import com.github.heheteam.commonlib.interfaces.AssignmentStorage
import com.github.heheteam.commonlib.interfaces.CourseId
import com.github.heheteam.commonlib.interfaces.CoursesDistributor
import com.github.heheteam.commonlib.interfaces.ProblemStorage
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.interfaces.StudentStorage
import com.github.heheteam.commonlib.interfaces.TeacherId
import com.github.heheteam.commonlib.interfaces.TeacherStorage
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
  coursesDistributor.addTeacherToCourse(TeacherId(2), realAnalysis)

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
