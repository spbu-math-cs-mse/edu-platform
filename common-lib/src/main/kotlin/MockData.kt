package com.github.heheteam.commonlib

val mockCoursesTable =
  mutableMapOf(
    "0" to Course("0", mutableListOf(), mutableListOf(), "Начала мат. анализа", MockGradeTable()),
    "1" to Course("1", mutableListOf(), mutableListOf(), "Теория вероятности", MockGradeTable()),
    "2" to Course("2", mutableListOf(), mutableListOf(), "Линейная алгебра", MockGradeTable()),
    "3" to Course("3", mutableListOf(), mutableListOf(), "ТФКП", MockGradeTable()),
  )

val mockStudentsTable =
  mutableMapOf(
    "0" to Student("0"),
    "1" to Student("1"),
    "2" to Student("2"),
    "3" to Student("3"),
  )

val mockAvailableCoursesTable =
  mutableMapOf(
    "0" to
      mutableMapOf(
        "0" to Pair(mockCoursesTable["0"]!!, true),
        "1" to Pair(mockCoursesTable["1"]!!, false),
        "2" to Pair(mockCoursesTable["2"]!!, false),
        "3" to Pair(mockCoursesTable["3"]!!, true),
      ),
  )
