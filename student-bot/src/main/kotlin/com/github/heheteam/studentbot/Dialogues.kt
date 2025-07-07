package com.github.heheteam.studentbot

import com.github.heheteam.commonlib.Course
import com.github.heheteam.commonlib.errors.TokenError
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.message.textsources.TextSource
import dev.inmo.tgbotapi.utils.buildEntities
import dev.inmo.tgbotapi.utils.code

object Dialogues {
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

  const val devAskForId: String = "Введите свой id:"

  const val devIdNotFound: String = "Этот id не был найден в базе данных! Попробуйте ещё раз:"

  const val devIdIsNotLong: String = "Некорректный id - он должен быть числом! Попробуйте ещё раз:"

  const val askCourseForSubmission: String = "Выбери курс для отправки решения:"

  const val askProblem: String = "Выбери задачу:"

  const val tellSubmissionIsSent: String = "Решение отправлено на проверку!"

  const val tellValidSubmissionTypes: String =
    "Отправь фото, файл или напиши решение текстом, и я отошлю его на проверку!"

  const val tellSubmissionTypeIsInvalid: String =
    "Данный формат не подходит, попробуй другой!\n" + tellValidSubmissionTypes

  const val tellToApplyForCourses: String = "Сначала запишитесь на курсы!"

  fun successfullyRegisteredForCourse(course: Course, token: String): String =
    "Вы успешно записались на курс ${course.name}, используя токен $token"

  fun failedToRegisterForCourse(error: TokenError): String =
    "Не удалось записаться на курс.\n" + error.toReadableString()

  fun sendStudentId(studentId: StudentId): List<TextSource> = buildEntities {
    +"Ваш ID: "
    code("$studentId")
  }
}
