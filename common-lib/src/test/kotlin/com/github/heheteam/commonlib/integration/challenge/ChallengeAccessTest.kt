package com.github.heheteam.commonlib.integration.challenge

import com.github.heheteam.commonlib.Assignment
import com.github.heheteam.commonlib.ProblemDescription
import com.github.heheteam.commonlib.Student
import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.util.TestDataBuilder
import com.github.heheteam.commonlib.util.buildData
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ChallengeAccessTest : IntegrationTestEnvironment() {
  lateinit var challenge1: Assignment
  lateinit var challenge2: Assignment

  private suspend fun TestDataBuilder.createTestCourse(student: Student) =
    course("Course1") {
      withStudent(student)
      val (assignment1, _) = assignment("Assignment1") { problem("Problem1", 10) }

      challenge1 = challenge(assignment1, "Challenge1") { problem("HardProblem1", 20) }.first

      val (assignment2, _) = assignment("Assignment2") { problem("Problem2", 10) }

      challenge2 = challenge(assignment2, "Challenge2") { problem("HardProblem2", 20) }.first
    }

  @Test
  fun `student does not have an access to the challenges by default`() = runTest {
    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val course = createTestCourse(student)

      val studentProblems = apis.studentApi.getActiveProblems(student.id, course.id).value
      assertFalse(studentProblems.contains(challenge1))
      assertFalse(studentProblems.contains(challenge2))
    }
  }

  @Test
  fun `student has access to the challenges when access is granted`() = runTest {
    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val course = createTestCourse(student)

      apis.adminApi.grantAccessToChallengeForStudent(student.id, course.id)

      val studentProblems = apis.studentApi.getActiveProblems(student.id, course.id).value

      assertContains(studentProblems, challenge1)
      assertContains(studentProblems, challenge2)
    }
  }

  fun createProblems(n: Int) = (1..n).map { ProblemDescription(it, it.toString(), "problem $it") }

  @Test
  fun `student has no access to the new challenges`() = runTest {
    buildData(createDefaultApis()) {
      val student = student("Student1", "Student1")
      val course = createTestCourse(student)

      apis.adminApi.grantAccessToChallengeForStudent(student.id, course.id)

      val assignment3Id =
        apis.adminApi.createAssignment(course.id, "Assignment3", createProblems(5), null).value
      val challenge3Id =
        apis.adminApi
          .createChallenge(course.id, assignment3Id, "Challenge3", createProblems(5), null)
          .value

      val studentProblems = apis.studentApi.getActiveProblems(student.id, course.id).value

      assertFalse(studentProblems.keys.map { it.id }.contains(challenge3Id))
    }
  }
}
