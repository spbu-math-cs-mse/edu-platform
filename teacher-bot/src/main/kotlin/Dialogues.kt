package com.github.heheteam.teacherbot
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import com.github.heheteam.commonlib.*

object Dialogues {
  val okSticker = InputFile.fromId("CAACAgEAAxkBAAIBJWcUPyqe-UEVGqMmhNYi21U3gkBEAALrAQACOA6CEbOGBM7hnEk5NgQ")
  val typingSticker = InputFile.fromId("CAACAgEAAxkBAAIBb2cdPy6r60MpNFXFLAABaQWOqfgCHAAC6QADZszART3es5n3X_q7NgQ")
  val greetingSticker = InputFile.fromId("CAACAgEAAxkBAAIBbmcdPydqt93f8S1XKHV3z73nUoLgAALxAQACOA6CEXTVKqzkcGAkNgQ")

  fun greetings(): String =
    "Здравствуйте! Я бот-помощник для преподавателей.\n" +
      "С моей помощью вы сможете:\n" +
      "• Получать решения учеников на проверку\n" +
      "• Отправлять обратную связь\n" +
      "• Выставлять оценки\n"

  fun askFirstName(): String = "Как я могу к вам обращаться? Напишите ваше имя."

  fun askLastName(firstName: String): String = "Отлично, $firstName, введите вашу фамилию \uD83D\uDC47"

  fun askGrade(
    firstName: String,
    lastName: String,
  ): String = "Рад знакомству, $firstName $lastName!\nУкажите ваши предметы преподавания"

  fun solutionSent(): String = "Готово!"

  fun solutionNotSent(): String = "Ошибка, попробуйте ещё раз..."

  // TODO: add more of student and problem info
  fun solutionInfo(solution: Solution): String = "Ученик отправил задачу ${solution.problem.id}"

  fun menu(): String = "\u2705 Главное меню"

  fun testSendSolution(): String =
    "ТЕСТ_Отправьте решение:"

  fun noSolutionsToCheck(): String =
    "Задач для проверки нет!"
}
