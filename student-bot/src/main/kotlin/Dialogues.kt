package com.github.heheteam.studentbot

import dev.inmo.tgbotapi.requests.abstracts.InputFile

object Dialogues {
  val greetingSticker = InputFile.fromId("CAACAgEAAxkBAAIBbmcdPydqt93f8S1XKHV3z73nUoLgAALxAQACOA6CEXTVKqzkcGAkNgQ")

  fun greetings(): String =
    "Привет! Я бот-помощник для записи на курсы.\n" +
      "Я помогу тебе:\n" +
      "• Записаться на интересующие курсы\n" +
      "• Посмотреть курсы, на которые ты уже записан\n" +
      "• Отправлять решения задач\n" +
      "• Получать обратную связь от преподавателей"

  fun askFirstName(): String = "Как я могу к тебе обращаться? Напиши свое имя."

  fun askLastName(firstName: String): String = "Отлично, $firstName, введи свою фамилию \uD83D\uDC47"

  fun askGrade(
    firstName: String,
    lastName: String,
  ): String = "Рад знакомству, $firstName $lastName!\nВ каком классе ты учишься?"

  fun askCourseForSolution(): String = "Выбери курс для отправки решения:"

  fun tellSolutionIsSent(): String = "Решение отправлено на проверку!"

  fun tellValidSolutionTypes(): String = "Отправь фото, файл или напиши решение текстом, и я отошлю его на проверку!"

  fun tellSolutionTypeIsInvalid(): String = "Данный формат не подходит, попробуй другой!"

  fun tellToApplyForCourses(): String = "Сначала запишитесь на курсы!"
}