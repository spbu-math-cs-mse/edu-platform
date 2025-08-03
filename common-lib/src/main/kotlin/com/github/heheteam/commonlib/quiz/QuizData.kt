package com.github.heheteam.commonlib.quiz

import com.github.heheteam.commonlib.interfaces.QuizId
import com.github.heheteam.commonlib.interfaces.StudentId

data class StudentAnswer(val quizId: QuizId, val studentId: StudentId, val chosenAnswerIndex: Int)

data class StudentOverCourseResults(val totalQuizzes: Int, val rightAnswers: Int)
