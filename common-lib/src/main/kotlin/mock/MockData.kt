package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student

val mockCoursesTable =
  mutableMapOf(
    "0" to
      Course(
        "0",
        description = "Начала мат. анализа",
        gradeTable = MockGradeTable(),
        assignments =
        mutableListOf(
          Assignment(
            "0",
            "Серия 1",
            mutableListOf(Problem("0", "1", "", 10, "0"), Problem("1", "2", "", 5, "0")),
            "0",
          ),
        ),
      ),
    "1" to
      Course(
        "1",
        description = "Теория вероятности",
        gradeTable = MockGradeTable(),
        assignments = mutableListOf(
          Assignment(
            "1", "Серия 1",
            mutableListOf(
              Problem("2", "1", "", 10, "1"),
            ),
            "1",
          ),
        ),
      ),
    "2" to
      Course(
        "2",
        description = "Линейная алгебра",
        gradeTable = MockGradeTable(),
        assignments =
        mutableListOf(
          Assignment(
            "2",
            "Серия 1",
            mutableListOf(
              Problem("3", "1", "", 10, "2"),
              Problem("4", "2", "", 5, "2"),
              Problem("5", "3", "", 5, "2"),
            ),
            "2",
          ),
        ),
      ),
    "3" to
      Course(
        "3",
        description = "Теория функции комплексной переменной",
        gradeTable = MockGradeTable(),
        assignments =
        mutableListOf(
          Assignment(
            "3",
            "Серия 1",
            mutableListOf(Problem("6", "1", "", 1, "3"), Problem("7", "2", "", 5, "3")),
            "3",
          ),
        ),
      ),
  )

val mockStudentsTable =
  mutableMapOf(
    "0" to Student("0", "Мария", "Кузнецова"),
    "1" to Student("1", "Иван", "Баландин"),
    "2" to Student("2"),
    "3" to Student("3"),
  )

// Student.id -> MutableList<Course.id>
val mockStudentsAndCourses =
  mutableMapOf(
    "0" to mutableListOf("1", "2"),
    "1" to mutableListOf("0", "3"),
  )

// Teacher.id -> Set<(Course.id, boolean)>
val MockDoesTeacherHaveAccessToCourse: MutableMap<String, MutableSet<Pair<String, Boolean>>> = mutableMapOf()

fun Problem.getAssignment(
  assignments: MutableList<Assignment> =
    mockCoursesTable.values
      .flatMap {
        it.assignments
      }.toMutableList(),
): Assignment? = assignments.find { it.id == assignmentId }

fun Assignment.getCourse(courses: MutableList<Course> = mockCoursesTable.values.toMutableList()): Course? = courses.find { it.id == courseId }
