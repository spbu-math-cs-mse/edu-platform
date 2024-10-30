package com.github.heheteam.samplebot

import Parent
import Solution
import Student

val mockSolutions: MutableList<Solution> = mutableListOf()

var mockTgUsername: String = ""

val mockTeachers: MutableMap<String, Parent> by lazy {
    mutableMapOf(
        mockTgUsername to Parent("1", listOf(Student("1"), Student("2"), Student("4"))),
        "@somebody" to Parent("2", listOf(Student("3"))),
    )
}