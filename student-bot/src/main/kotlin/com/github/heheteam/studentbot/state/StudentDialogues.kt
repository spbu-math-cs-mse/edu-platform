package com.github.heheteam.studentbot.state

import dev.inmo.tgbotapi.requests.abstracts.InputFile

object StudentDialogues {
  val greetingSticker =
    InputFile.fromId("CAACAgEAAxkBAAIBbmcdPydqt93f8S1XKHV3z73nUoLgAALxAQACOA6CEXTVKqzkcGAkNgQ")
  val okSticker =
    InputFile.fromId("CAACAgEAAxkBAAIBJWcUPyqe-UEVGqMmhNYi21U3gkBEAALrAQACOA6CEbOGBM7hnEk5NgQ")
  val typingSticker =
    InputFile.fromId("CAACAgEAAxkBAAIBb2cdPy6r60MpNFXFLAABaQWOqfgCHAAC6QADZszART3es5n3X_q7NgQ")
  val heartSticker =
    InputFile.fromId("CAACAgEAAxkBAAIBqGcePu_DPJQIwyU2hfH7SbMJ1g_DAAL6AQACOA6CEZdviQ02NivYNgQ")
  val nerdSticker =
    InputFile.fromId("CAACAgEAAxkBAAIBxGceRcgL_nCWjiRrMzWQSi_MdlDzAAL4AQACOA6CEVv05nratTJkNgQ")

  const val greetings: String =
    "Привет! Я бот-помощник для записи на курсы.\n" +
      "Я помогу тебе:\n" +
      "• Записаться на интересующие курсы\n" +
      "• Посмотреть курсы, на которые ты уже записан\n" +
      "• Отправлять решения задач\n" +
      "• Получать обратную связь от преподавателей\n"

  fun niceToMeetYou(firstName: String, lastName: String): String =
    "Приятно познакомиться, $firstName $lastName!\n"

  const val askFirstName: String = "Как я могу к тебе обращаться? Напиши свое имя."

  fun askLastName(firstName: String): String =
    "Отлично, $firstName, введи свою фамилию \uD83D\uDC47"

  fun askGrade(firstName: String, lastName: String): String =
    "Рад знакомству, $firstName $lastName!\nВ каком классе ты учишься?"

  const val menu: String = "\u2705 Главное меню"

  const val aboutCourse: String = "\uD83D\uDCD6 Подробнее о курсе"
}
