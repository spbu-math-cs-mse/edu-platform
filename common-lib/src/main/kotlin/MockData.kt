package com.github.heheteam.commonlib

val mockCoursesTable =
  mutableMapOf(
    "0" to
      Course(
        "0",
        description = "Начала мат. анализа",
        gradeTable = MockGradeTable(),
        series =
        mutableListOf(
          Series(
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
        series = mutableListOf(Series("1", "Серия 1", mutableListOf(Problem("2", "1", "", 10, "1")), "1")),
      ),
    "2" to
      Course(
        "2",
        description = "Линейная алгебра",
        gradeTable = MockGradeTable(),
        series =
        mutableListOf(
          Series(
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
        series =
        mutableListOf(
          Series(
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

val mockAvailableCoursesTable =
  mutableMapOf(
    "0" to
      mutableMapOf(
        "0" to Pair(mockCoursesTable["0"]!!, true),
        "1" to Pair(mockCoursesTable["1"]!!, false),
        "2" to Pair(mockCoursesTable["2"]!!, false),
        "3" to Pair(mockCoursesTable["3"]!!, true),
      ),
    "1" to
      mutableMapOf(
        "0" to Pair(mockCoursesTable["0"]!!, false),
        "1" to Pair(mockCoursesTable["1"]!!, true),
        "2" to Pair(mockCoursesTable["2"]!!, true),
        "3" to Pair(mockCoursesTable["3"]!!, false),
      ),
  )

// Student.id -> MutableList<Course.id>
val mockStudentsAndCourses =
  mutableMapOf(
    "0" to mutableListOf("1", "2"),
    "1" to mutableListOf("0", "3"),
  )

var mockIncrementalSolutionId = 0
var wasMockGradeTableForTeacherBuilt = false

// Teacher.id -> Set<(Course.id, boolean)>
val MockDoesTeacherHaveAccessToCourse: MutableMap<String, MutableSet<Pair<String, Boolean>>> = mutableMapOf()

fun Problem.getSeries(
  series: MutableList<Series> =
    mockCoursesTable.values
      .flatMap {
        it.series
      }.toMutableList(),
): Series? = series.find { it.id == seriesId }

fun Series.getCourse(courses: MutableList<Course> = mockCoursesTable.values.toMutableList()): Course? = courses.find { it.id == courseId }
