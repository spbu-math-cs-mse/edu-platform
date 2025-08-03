package com.github.heheteam.adminbot

import com.github.heheteam.adminbot.states.assignments.parseProblemsDescriptions
import com.github.heheteam.commonlib.ProblemDescription
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTest {

  @Test
  fun parsingProblemsDescriptionsTest() {
    var problemsDescriptions =
      "1\n" + "2 \"\" 5\n" + "3a \"Лёгкая задача\"\n" + "3b \"Сложная задача\" 10"
    val parsedProblemsDescriptions = parseProblemsDescriptions(problemsDescriptions)
    assertTrue(parsedProblemsDescriptions.isOk)
    val expectedProblemsDescriptions =
      listOf(
        ProblemDescription(1, "1"),
        ProblemDescription(2, "2", maxScore = 5),
        ProblemDescription(3, "3a", "Лёгкая задача"),
        ProblemDescription(4, "3b", "Сложная задача", 10),
      )
    assertEquals(expectedProblemsDescriptions, parsedProblemsDescriptions.value)

    problemsDescriptions =
      "1 2 3 4\n" + "2 \"\" 5\n" + "3a \"Лёгкая задача\"\n" + "3b \"Сложная задача\" 10"
    assertTrue(parseProblemsDescriptions(problemsDescriptions).isErr)

    problemsDescriptions = "1\n" + "\n" + "3a \"Лёгкая задача\"\n" + "3b \"Сложная задача\" 10"
    assertTrue(parseProblemsDescriptions(problemsDescriptions).isErr)

    problemsDescriptions =
      "1\n" + "2 \"\" b\n" + "3a \"Лёгкая задача\"\n" + "3b \"Сложная задача\" 10"
    assertTrue(parseProblemsDescriptions(problemsDescriptions).isErr)
  }
}
