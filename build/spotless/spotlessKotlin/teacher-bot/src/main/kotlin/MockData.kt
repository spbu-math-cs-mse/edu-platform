package com.github.heheteam.samplebot

import Solution
import Teacher

val mockSolutions: MutableList<Solution> = mutableListOf()

var mockTgUsername: String = ""

val mockTeachers: MutableMap<String, Teacher> by lazy {
  mutableMapOf(
    mockTgUsername to Teacher("1"),
    "@somebody" to Teacher("2"),
  )
}
