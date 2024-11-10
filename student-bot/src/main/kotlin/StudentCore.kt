package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.SolutionContent
import com.github.heheteam.commonlib.SolutionDistributor
import com.github.heheteam.commonlib.Student

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
