package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.api.*
import com.github.heheteam.commonlib.database.reset
import org.jetbrains.exposed.sql.Database

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
    studentStorage: StudentStorage,
    teacherStorage: TeacherStorage,
    database: Database,
): List<CourseId> {
    reset(database)
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
    val students = listOf(
        "Алексей" to "Иванов",
        "Мария" to "Петрова",
        "Дмитрий" to "Сидоров",
        "Анна" to "Смирнова",
        "Иван" to "Кузнецов",
        "Елена" to "Попова",
        "Виктор" to "Семенов",
        "Ольга" to "Соколова",
        "Андрей" to "Михайлов",
        "Николай" to "Васильев",
    ).map { studentStorage.createStudent(it.first, it.second) }
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
    println("first student is ${studentStorage.resolveStudent(students.first())}")

    listOf(
        "Григорий" to "Лебедев",
        "Егор" to "Тихонов",
    ).map { teacherStorage.createTeacher(it.first, it.second) }

    return listOf(realAnalysis, probTheory, linAlgebra, complAnalysis)
}
