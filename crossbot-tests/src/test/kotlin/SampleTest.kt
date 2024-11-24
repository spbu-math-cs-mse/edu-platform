package com.github.heheteam.commonlib

import kotlin.test.Test
import kotlin.test.assertEquals

class SampleTest {
  @Test
  fun `sample tests`() {
    val problem = Problem("p1", "1", "d1", 1, "assgn1")
    assertEquals(problem.id, "p1")
  }
}
