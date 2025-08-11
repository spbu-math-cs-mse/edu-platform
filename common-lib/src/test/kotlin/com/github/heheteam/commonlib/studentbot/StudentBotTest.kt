package com.github.heheteam.commonlib.studentbot

import com.github.heheteam.commonlib.integration.IntegrationTestEnvironment
import com.github.heheteam.commonlib.util.buildData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class StudentBotTest : IntegrationTestEnvironment() {

  @Test
  fun `new student courses assignment test`() = runTest {
    buildData(createDefaultApis()) {
      val studentId = apis.studentApi.createStudent("name", "surname", 100L, 5, null).value

      val studentCourses = studentApi.getStudentCourses(studentId).value
      assertEquals(listOf(), studentCourses.map { it.id }.sortedBy { it.long })
    }
  }
}
