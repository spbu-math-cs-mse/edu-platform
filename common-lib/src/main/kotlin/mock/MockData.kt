package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Student

val mockCoursesTable =
  mutableMapOf(
    0L to
      Course(
        0L,
        description = "Начала мат. анализа",
        gradeTable = MockGradeTable(),
        assignments =
        mutableListOf(
          Assignment(
            0L,
            "Серия 1",
            mutableListOf(Problem(0, "1", "", 10, 0L), Problem(1, "2", "", 5, 0L)),
            0L,
          ),
        ),
      ),
    "1" to
      Course(
        1L,
        description = "Теория вероятности",
        gradeTable = MockGradeTable(),
        assignments = mutableListOf(
          Assignment(
            1L, "Серия 1",
            mutableListOf(
              Problem(2L, "1", "", 10, 1L),
            ),
            1L,
          ),
        ),
      ),
    2L to
      Course(
        2L,
        description = "Линейная алгебра",
        gradeTable = MockGradeTable(),
        assignments =
        mutableListOf(
          Assignment(
            2L,
            "Серия 1",
            mutableListOf(
              Problem(3L, "1", "", 10, 2L),
              Problem(4L, "2", "", 5, 2L),
              Problem(5L, "3", "", 5, 2L),
            ),
            2L,
          ),
        ),
      ),
    3L to
      Course(
        3L,
        description = "Теория функции комплексной переменной",
        gradeTable = MockGradeTable(),
        assignments =
        mutableListOf(
          Assignment(
            3L,
            "Серия 1",
            mutableListOf(Problem(6L, "1", "", 1, 3L), Problem(7L, "2", "", 5, 3L)),
            3L,
          ),
        ),
      ),
  )

val mockStudentsTable =
  mutableMapOf(
    0L to Student(0, "Мария", "Кузнецова"),
    1L to Student(1, "Иван", "Баландин"),
    2L to Student(2),
    3L to Student(3),
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
