package com.github.heheteam.studentbot

import SolutionContent
import SolutionDistributor
import Student

class StudentCore(
  private val solutionDistributor: SolutionDistributor,
  private val usernamesRegistry: UsernamesRegistry,
) : UsernamesRegistry by usernamesRegistry {
  fun sendSolution(
    username: String,
    solutionContent: SolutionContent,
  ) {
    solutionDistributor.inputSolution(Student(getUserId(username)), solutionContent)
  }
}

interface UsernamesRegistry {
  fun getUserId(username: String): String

  fun setUserId(
    username: String,
    id: String,
  )
}
