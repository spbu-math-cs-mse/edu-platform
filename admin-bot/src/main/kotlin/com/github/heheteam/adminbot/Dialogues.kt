package com.github.heheteam.adminbot

import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.message.textsources.TextSourcesList
import dev.inmo.tgbotapi.types.message.textsources.code
import dev.inmo.tgbotapi.types.message.textsources.plus
import dev.inmo.tgbotapi.types.message.textsources.regularln

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

  const val askFirstName: String = "Как я могу к вам обращаться? Напишите ваше имя."

  fun askLastName(firstName: String): String =
    "Отлично, $firstName, введите вашу фамилию \uD83D\uDC47"

  fun niceToMeetYou(firstName: String, lastName: String): String =
    "Приятно познакомиться, $firstName $lastName!\n"

  fun notFoundInWhitelist(tgId: Long) =
    "Ваш id не был добавлен в систему. Попросите администратора добавить вас по id $tgId. "

  const val greetings: String =
    "Привет! Я бот-помощник для управления образовательным процессом.\n" +
      "Я помогу тебе:\n" +
      "• Записаться на интересующие курсы\n" +
      "• Посмотреть курсы, на которые ты уже записан\n" +
      "• Отправлять решения задач\n" +
      "• Получать обратную связь от преподавателей"

  const val devAskForId: String = "Введите свой id:"

  fun adminIdIsNotInWhitelist(tgId: Long): TextSourcesList =
    regularln(
      "Вашего аккаунта нет в списке разрешенных. Попросите админа вас добавить.\n\nВаш Telegram id: "
    ) + code(tgId.toString())

  const val devIdIsNotLong: String = "Некорректный id - он должен быть числом! Попробуйте ещё раз:"

  const val noIdInInput: String = "Вы не ввели ни одного id. Попробуйте ещё раз!"

  fun oneIdAlreadyExistsForStudentAddition(id: Long, courseName: String): String =
    "Ученик $id уже есть на курсе $courseName!"

  fun manyIdsAlreadyExistForStudentAddition(ids: List<Long>, courseName: String): String =
    "Ученики с id ${ids.joinToString()} уже есть на курсе $courseName!"

  fun oneStudentIdDoesNotExist(id: Long): String = "Ученика $id не существует. Попробуйте ещё раз!"

  fun manyStudentIdsDoNotExist(ids: List<Long>): String =
    "Учеников с id ${ids.joinToString()} не существует. Попробуйте ещё раз!"

  fun oneIdIsGoodForStudentAddition(id: Long, courseName: String): String =
    "Ученик $id успешно добавлен на курс $courseName!"

  fun manyIdsAreGoodForStudentAddition(ids: List<Long>, courseName: String): String =
    "Ученики с id ${ids.joinToString()} успешно добавлены на курс $courseName!"

  fun oneIdAlreadyExistsForTeacherAddition(id: Long, courseName: String): String =
    "Преподаватель $id уже есть на курсе $courseName!"

  fun manyIdsAlreadyExistForTeacherAddition(ids: List<Long>, courseName: String): String =
    "Преподаватели с id ${ids.joinToString()} уже есть на курсе $courseName!"

  fun oneTeacherIdDoesNotExist(id: Long): String =
    "Преподавателя $id не существует. Попробуйте ещё раз!"

  fun manyTeacherIdsDoNotExist(ids: List<Long>): String =
    "Преподавателей с id ${ids.joinToString()} не существует. Попробуйте ещё раз!"

  fun oneIdIsGoodForTeacherAddition(id: Long, courseName: String): String =
    "Преподаватель $id успешно добавлен на курс $courseName!"

  fun manyIdsAreGoodForTeacherAddition(ids: List<Long>, courseName: String): String =
    "Преподаватели с id ${ids.joinToString()} успешно добавлены на курс $courseName!"

  fun oneIdAlreadyDoesNotExistForStudentRemoving(id: Long, courseName: String): String =
    "Ученика $id нет на курсе $courseName!"

  fun manyIdsAlreadyDoNotExistForStudentRemoving(ids: List<Long>, courseName: String): String =
    "Учеников с id ${ids.joinToString()} нет на курсе $courseName!"

  fun oneIdIsGoodForStudentRemoving(id: Long, courseName: String): String =
    "Ученик $id успешно удалён с курса $courseName!"

  fun manyIdsAreGoodForStudentRemoving(ids: List<Long>, courseName: String): String =
    "Ученики с id ${ids.joinToString()} успешно удалены с курса $courseName!"

  fun oneIdAlreadyDoesNotExistForTeacherRemoving(id: Long, courseName: String): String =
    "Преподавателя $id нет на курсе $courseName!"

  fun manyIdsAlreadyDoNotExistForTeacherRemoving(ids: List<Long>, courseName: String): String =
    "Преподавателей с id ${ids.joinToString()} нет на курсе $courseName!"

  fun oneIdIsGoodForTeacherRemoving(id: Long, courseName: String): String =
    "Преподаватель $id успешно удалён с курса $courseName!"

  fun manyIdsAreGoodForTeacherRemoving(ids: List<Long>, courseName: String): String =
    "Преподаватели с id ${ids.joinToString()} успешно удалены с курса $courseName!"

  const val menu: String = "\u2705 Главное меню"

  const val askAssignmentDescription: String =
    "Введите название серии, которую хотите создать, и дедлайн по ней. " +
      "Например, \"Диффуры и не только\"\$2025-01-19T23:55:00"

  const val askProblemsDescriptions: String =
    "Введите описания задач, которые хотите добавить в серию, " +
      "в формате \'<номер> \"<описание>\" <максимальное кол-во баллов за задачу>\', " +
      "разделяя задачи переносом строки.\n" +
      "По умолчанию описание пустое, а максимальное кол-во баллов равно 1.\n\n" +
      "Пример:\n" +
      "1\n" +
      "2 \"\" 5\n" +
      "3a \"Лёгкая задача\"\n" +
      "3b \"Сложная задача\" 10"

  const val incorrectProblemDescriptionEmpty: String =
    "Некорректный формат ввода (обнаружена пустая строка). Попробуйте ещё раз!"

  fun incorrectProblemDescriptionTooManyArguments(problemDescription: String): String =
    "Некорректный формат ввода (обнаружена строчка ($problemDescription) с более, чем 3-мя аргументами). " +
      "Попробуйте ещё раз!"

  fun incorrectProblemDescriptionMaxScoreIsNotInt(maxScore: String): String =
    "Некорректный формат ввода (3-ий аргумент в какой-то строчке ($maxScore) не является числом). Попробуйте ещё раз!"

  const val assignmentWasCreatedSuccessfully: String = "Серия успешно создана!"

  const val addScheduledMessageStartSummary: String =
    "Вы начали процесс добавления отложенного сообщения. " +
      "Вам потребуется ввести текст сообщения, затем выбрать дату и время отправки. " +
      "Первая строка вашего сообщения будет использоваться как краткое описание."

  const val queryScheduledMessageContent: String =
    "Введите текст сообщения. Первая строка будет использоваться как краткое описание."
  const val scheduledMessageContentEmptyError: String = "Сообщение не может быть пустым."
  const val queryScheduledMessageDate: String = "Выберите дату или введите её в формате дд.мм.гггг"
  const val enterScheduledMessageDateManually: String =
    "Введите дату в формате дд.мм.гггг или /stop, чтобы отменить операцию."
  const val invalidDateFormat: String =
    "Неправильный формат даты. Введите дату в формате дд.мм.гггг или /stop, чтобы отменить операцию."
  const val invalidDateButtonFormat: String = "Неправильный формат даты из кнопки."
  const val queryScheduledMessageTime: String =
    "Введите время в формате чч:мм или /stop, чтобы отменить операцию."
  const val invalidTimeFormat: String =
    "Неправильный формат времени. Введите время в формате чч:мм или /stop, чтобы отменить операцию."
  const val confirmScheduledMessage: String = "Подтвердите отправку сообщения:"
  const val unknownCommand: String = "Неизвестная команда."
  const val operationCancelled: String = "Операция отменена."
}
