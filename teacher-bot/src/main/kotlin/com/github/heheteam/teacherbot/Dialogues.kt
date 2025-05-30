package com.github.heheteam.teacherbot

import dev.inmo.tgbotapi.requests.abstracts.InputFile

object Dialogues {
  val typingSticker =
    InputFile.fromId("CAACAgEAAxkBAAIBb2cdPy6r60MpNFXFLAABaQWOqfgCHAAC6QADZszART3es5n3X_q7NgQ")
  val greetingSticker =
    InputFile.fromId("CAACAgEAAxkBAAIBbmcdPydqt93f8S1XKHV3z73nUoLgAALxAQACOA6CEXTVKqzkcGAkNgQ")

  const val greetings: String =
    "Здравствуйте! Я бот-помощник для преподавателей.\n" +
      "С моей помощью вы сможете:\n" +
      "• Получать решения учеников на проверку\n" +
      "• Отправлять обратную связь\n" +
      "• Выставлять оценки\n"

  const val devAskForId: String = "Введите свой id:"

  const val devIdNotFound: String = "Этот id не был найден в базе данных! Попробуйте ещё раз:"

  const val devIdIsNotLong: String = "Некорректный id - он должен быть числом! Попробуйте ещё раз:"

  const val askFirstName: String = "Как я могу к вам обращаться? Напишите ваше имя."

  fun askLastName(firstName: String): String =
    "Отлично, $firstName, введите вашу фамилию \uD83D\uDC47"

  fun niceToMeetYou(firstName: String, lastName: String): String =
    "Приятно познакомиться, $firstName $lastName!\n"

  const val menu: String = "\u2705 Главное меню"
}
