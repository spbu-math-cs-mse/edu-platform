package com.github.heheteam.commonlib.quiz

sealed interface QuizActivationResult {
  object Success : QuizActivationResult

  object QuizNotFound : QuizActivationResult

  object QuizAlreadyActive : QuizActivationResult
}

sealed interface AnswerQuizResult {
  data class Success(val chosenAnswer: String) : AnswerQuizResult

  object QuizNotFound : AnswerQuizResult

  object QuizInactive : AnswerQuizResult

  data class QuizAnswerIndexOutOfBounds(val answerIndex: Int, val maxIndex: Int) : AnswerQuizResult
}

enum class QuizDeactivationStatus {
  Deactivated,
  NotDeactivated,
}
