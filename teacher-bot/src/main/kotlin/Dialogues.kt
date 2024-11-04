import dev.inmo.tgbotapi.requests.abstracts.InputFile

object Dialogues {
  val okSticker = InputFile.fromId("CAACAgEAAxkBAAIBJWcUPyqe-UEVGqMmhNYi21U3gkBEAALrAQACOA6CEbOGBM7hnEk5NgQ")
  val typingSticker = InputFile.fromId("CAACAgEAAxkBAAIBb2cdPy6r60MpNFXFLAABaQWOqfgCHAAC6QADZszART3es5n3X_q7NgQ")
  val greetingSticker = InputFile.fromId("CAACAgEAAxkBAAIBbmcdPydqt93f8S1XKHV3z73nUoLgAALxAQACOA6CEXTVKqzkcGAkNgQ")

  fun greetings(): String =
    "Привет! Меня зовут такса Дуся, и я отлично разбираюсь в олимпиадной математике.\n" +
      "Мои охотничьи инстинкты помогают мне находить классные олимпиадные задачи, " +
      "а превосходный нюх - отыскивать ошибки в твоих рассуждениях, гав-гав!\n\n" +
      "Свистни мне, когда захочешь порешать з-аф-аф-дачи! Также я помогу тебе не пропустить интересные" +
      " математические события - я тщательно за ними слежу!\n\n"

  fun askFirstName(): String = "Как я могу к тебе обращаться? Напиши свое имя."

  fun askLastName(firstName: String): String = "Отлично, $firstName, введи свою фамилию \uD83D\uDC47"

  fun solutionSent(): String = "Готово!"

  fun solutionNotSent(): String = "Ошибка, попробуйте ещё раз..."

  // TODO: add more of student and problem info
  fun solutionInfo(solution: Solution): String = "Ученик отправил задачу ${solution.problem.id}"

  fun askGrade(
    firstName: String,
    lastName: String,
  ): String = "Безумно рада нашему с тобой знакомству, $firstName $lastName!\nВ каком классе ты учишься?"

  fun menu(): String = "\u2705 Главное меню"

  fun testSendSolution(): String =
    "ТЕСТ_Отправьте решение:"

  fun noSolutionsToCheck(): String =
    "Задач для проверки нет!"
}
