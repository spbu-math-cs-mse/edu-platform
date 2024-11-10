package com.github.heheteam.teacherbot

import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.Teacher

val mockSolutions: MutableList<Solution> = mutableListOf()

var mockTgUsername: String = ""

val mockTeachers: MutableMap<String, Teacher> by lazy {
  mutableMapOf(
    mockTgUsername to Teacher("1"),
    "@somebody" to Teacher("2"),
  )
}
