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
        assignmentIds =
        listOf(
          Assignment(
            0L,
            "Серия 1",
            mutableListOf(Problem(0, "1", "", 10, 0L).id, Problem(1, "2", "", 5, 0L).id),
            0L,
          ).id,
        ),
      ),
    "1" to
      Course(
        1L,
        description = "Теория вероятности",
        gradeTable = MockGradeTable(),
        assignmentIds = listOf(
          Assignment(
            1L, "Серия 1",
            mutableListOf(
              Problem(2L, "1", "", 10, 1L).id,
            ),
            1L,
          ).id,
        ),
      ),
    2L to
      Course(
        2L,
        description = "Линейная алгебра",
        gradeTable = MockGradeTable(),
        assignmentIds =
        mutableListOf(
          Assignment(
            2L,
            "Серия 1",
            mutableListOf(
              Problem(3L, "1", "", 10, 2L).id,
              Problem(4L, "2", "", 5, 2L).id,
              Problem(5L, "3", "", 5, 2L).id,
            ),
            2L,
          ).id,
        ),
      ),
    3L to
      Course(
        3L,
        description = "Теория функции комплексной переменной",
        gradeTable = MockGradeTable(),
        assignmentIds =
        mutableListOf(
          Assignment(
            3L,
            "Серия 1",
            mutableListOf(Problem(6L, "1", "", 1, 3L).id, Problem(7L, "2", "", 5, 3L).id),
            3L,
          ).id,
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
