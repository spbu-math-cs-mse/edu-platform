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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val ASSIGNMENTS_PER_COURSE: Int = 2
private const val PROBLEMS_PER_ASSIGNMENT: Int = 5
private const val STUDENTS_PER_COURSE = 5

data class FillContent(
  val courses: List<CourseId>,
  val students: List<StudentId>,
  val teachers: List<TeacherId>,
)

class SampleGenerator : KoinComponent {
  private val coursesDistributor: CoursesDistributor by inject()
  private val problemStorage: ProblemStorage by inject()
  private val assignmentStorage: AssignmentStorage by inject()
  private val studentStorage: StudentStorage by inject()
  private val teacherStorage: TeacherStorage by inject()
  private val database: Database by inject()

  private fun generateCourse(name: String): CourseId {
    val courseId = coursesDistributor.createCourse(name)
    (1..ASSIGNMENTS_PER_COURSE).map { assgnNum ->
      assignmentStorage.createAssignment(
        courseId,
        "assignment $courseId.$assgnNum",
        (1..PROBLEMS_PER_ASSIGNMENT).map { ProblemDescription(it, "$assgnNum.$it", "", 1, null) },
        problemStorage,
      )
    }
    return courseId
  }

  fun fillWithSamples(): FillContent {
    reset(database)
    val realAnalysis = generateCourse("Начала мат. анализа")
    val probTheory = generateCourse("Теория вероятностей")
    val linAlgebra = generateCourse("Линейная алгебра")
    val complAnalysis = generateCourse("ТФКП")
    val students = createStudents(studentStorage)
    students.slice(0..<STUDENTS_PER_COURSE).map { studentId ->
      coursesDistributor.addStudentToCourse(studentId, realAnalysis)
      coursesDistributor.addStudentToCourse(studentId, probTheory)
    }
    students.slice(STUDENTS_PER_COURSE..<2 * STUDENTS_PER_COURSE).map { studentId ->
      coursesDistributor.addStudentToCourse(studentId, probTheory)
      coursesDistributor.addStudentToCourse(studentId, linAlgebra)
    }
    println("first student is ${studentStorage.resolveStudent(students.first())}")

    val teachers =
      listOf("Мария" to "Соколова", "Егор" to "Тихонов").map {
        teacherStorage.createTeacher(it.first, it.second).also { teacherId ->
          coursesDistributor.addTeacherToCourse(teacherId, realAnalysis)
        }
      }

    return FillContent(
      courses = listOf(realAnalysis, probTheory, linAlgebra, complAnalysis),
      students = students,
      teachers = teachers,
    )
  }

  private fun createStudents(studentStorage: StudentStorage): List<StudentId> {
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
}
