package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.api.AssignmentStorage
import com.github.heheteam.commonlib.api.CourseId
import com.github.heheteam.commonlib.api.CoursesDistributor
import com.github.heheteam.commonlib.api.ProblemStorage
import com.github.heheteam.commonlib.api.StudentStorage
import com.github.heheteam.commonlib.api.TeacherId
import com.github.heheteam.commonlib.api.TeacherStorage
import com.github.heheteam.commonlib.database.reset
import org.jetbrains.exposed.sql.Database
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SampleGenerator : KoinComponent {
  private val coursesDistributor: CoursesDistributor by inject()
  private val problemStorage: ProblemStorage by inject()
  private val assignmentStorage: AssignmentStorage by inject()
  private val studentStorage: StudentStorage by inject()
  private val teacherStorage: TeacherStorage by inject()
  private val database: Database by inject()

  private fun generateCourse(
    name: String,
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

  fun fillWithSamples(): List<CourseId> {
    reset(database)
    val realAnalysis = generateCourse("Начала мат. анализа")
    val probTheory = generateCourse("Теория вероятностей")
    val linAlgebra = generateCourse("Линейная алгебра")
    val complAnalysis = generateCourse("ТФКП")
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
    students.slice(0..<5).map { studentId ->
      coursesDistributor.addStudentToCourse(studentId, realAnalysis)
    }
    students.slice(0..<5).map { studentId ->
      coursesDistributor.addStudentToCourse(studentId, probTheory)
    }
    students.slice(5..<10).map { studentId ->
      coursesDistributor.addStudentToCourse(studentId, probTheory)
    }
    students.slice(5..<10).map { studentId ->
      coursesDistributor.addStudentToCourse(studentId, linAlgebra)
    }
    println("first student is ${studentStorage.resolveStudent(students.first())}")

    listOf("Павел" to "Мозоляко", "Егор" to "Тихонов").map {
      teacherStorage.createTeacher(it.first, it.second)
    }

    coursesDistributor.addTeacherToCourse(TeacherId(1), realAnalysis)

    return listOf(realAnalysis, probTheory, linAlgebra, complAnalysis)
  }
}
