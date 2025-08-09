package com.github.heheteam.adminbot.states.challenges

import com.github.heheteam.adminbot.Dialogues
import com.github.heheteam.commonlib.ProblemDescription
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr

// This messy code is completely covered by tests,
// and I hope it will be removed in the future in favor of creating a new UI
@Suppress("MagicNumber")
fun parseProblemsDescriptions(
  problemsDescriptionsFromText: String
): Result<List<ProblemDescription>, String> {
  val problemsDescriptions = mutableListOf<ProblemDescription>()
  problemsDescriptionsFromText.lines().mapIndexed { index, problemDescription ->
    val arguments =
      """[^\s"]+|"([^"]*)""""
        .toRegex()
        .findAll(problemDescription)
        .map { it.groups[1]?.value ?: it.value }
        .toList()

    val maxScore =
      when {
        arguments.isEmpty() -> {
          Err(Dialogues.incorrectProblemDescriptionEmpty)
        }

        arguments.size > 3 -> {
          Err(Dialogues.incorrectProblemDescriptionTooManyArguments(problemDescription))
        }

        else -> {
          arguments
            .elementAtOrElse(2) { "1" }
            .toIntOrNull()
            .toResultOr { Dialogues.incorrectProblemDescriptionMaxScoreIsNotInt(arguments.last()) }
        }
      }
    if (maxScore.isErr) {
      return Err(maxScore.error)
    }
    problemsDescriptions.add(
      ProblemDescription(
        index + 1,
        arguments.first(),
        arguments.elementAtOrElse(1) { "" },
        maxScore.value,
      )
    )
  }
  return Ok(problemsDescriptions)
}
