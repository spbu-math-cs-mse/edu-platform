package com.github.heheteam.adminbot

import dev.inmo.tgbotapi.requests.abstracts.InputFile

object Dialogues {
  val greetingSticker = InputFile.fromId("CAACAgEAAxkBAAIBbmcdPydqt93f8S1XKHV3z73nUoLgAALxAQACOA6CEXTVKqzkcGAkNgQ")
  val okSticker = InputFile.fromId("CAACAgEAAxkBAAIBJWcUPyqe-UEVGqMmhNYi21U3gkBEAALrAQACOA6CEbOGBM7hnEk5NgQ")
  val typingSticker = InputFile.fromId("CAACAgEAAxkBAAIBb2cdPy6r60MpNFXFLAABaQWOqfgCHAAC6QADZszART3es5n3X_q7NgQ")
  val heartSticker = InputFile.fromId("CAACAgEAAxkBAAIBqGcePu_DPJQIwyU2hfH7SbMJ1g_DAAL6AQACOA6CEZdviQ02NivYNgQ")
  val nerdSticker = InputFile.fromId("CAACAgEAAxkBAAIBxGceRcgL_nCWjiRrMzWQSi_MdlDzAAL4AQACOA6CEVv05nratTJkNgQ")

  fun greetings(): String =
    "Привет! Я бот-помощник для управления образовательным процессом.\n" +
      "Я помогу тебе:\n" +
      "• Записаться на интересующие курсы\n" +
      "• Посмотреть курсы, на которые ты уже записан\n" +
      "• Отправлять решения задач\n" +
      "• Получать обратную связь от преподавателей"

  fun devAskForId(): String = "Введите свой id:"
  fun devIdNotFound(): String = "Этот id не был найден в базе данных! Попробуйте ещё раз:"

  fun menu(): String = "\u2705 Главное меню"

  fun noCoursesWasFound(): String = "Не было найдено никаких курсов!"
  fun noCoursesWasFoundForCreationOfAssignment(): String = "Сначала создайте какой-нибудь курс!"
  fun assignmentDescriptionIsNotText(): String = "Название серии должно являться текстом. Отправьте текстовое сообщение!"
  fun problemsDescriptionsAreNotTexts(): String = "Описания задач должны быть представлены в текстовом виде. Отправьте текстовое сообщение!"
  fun askCourse(): String = "Выберите курс:"
  fun askAssignmentDescription(): String = "Введите название серии, которую хотите создать."
  fun askProblemsDescriptions(): String =
    "Введите описания задач, которые хотите добавить в серию, " +
      "в формате \'<номер> \"<описание>\" <максимальное кол-во баллов за задачу>\', " +
      "разделяя задачи переносом строки.\n" +
      "По умолчанию описание пустое, а максимальное кол-во баллов равно 1.\n\n" +
      "Пример:\n" +
      "1\n" +
      "2 \"\" 5\n" +
      "3a \"Лёгкая задача\"\n" +
      "3b \"Сложная задача\" 10"
  fun incorrectProblemDescriptionEmpty(): String = "Некорректный формат ввода (обнаружена пустая строка). Попробуйте ещё раз!"
  fun incorrectProblemDescriptionTooManyArguments(problemDescription: String): String = "Некорректный формат ввода (обнаружена строчка ($problemDescription) с более, чем 3-мя аргументами). Попробуйте ещё раз!"
  fun incorrectProblemDescriptionMaxScoreIsNotInt(maxScore: String): String = "Некорректный формат ввода (3-ий аргумент в какой-то строчке ($maxScore) не является числом). Попробуйте ещё раз!"
  fun assignmentWasCreatedSuccessfully(): String = "Серия успешно создана!"
}
