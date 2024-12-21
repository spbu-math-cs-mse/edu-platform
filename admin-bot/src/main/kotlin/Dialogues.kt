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
  fun idIsNotLong(id: String): String =
    "Обнаружен некорректный id — \"$id\".\nid должен быть числом. Попробуйте ещё раз!"

  fun duplicatedId(id: String): String =
    "Обнаружен id, который встретился несколько раз, — \"$id\".\nВсе id должны быть уникальными. Попробуйте ещё раз!"

  fun noIdInInput(): String = "Вы не ввели ни одного id. Попробуйте ещё раз!"
  fun oneIdAlreadyExistsForStudentAddition(id: Long, courseName: String): String = "Ученик $id уже есть на курсе $courseName!"
  fun manyIdsAlreadyExistForStudentAddition(ids: List<Long>, courseName: String): String = "Ученики с id ${ids.joinToString()} уже есть на курсе $courseName!"
  fun oneStudentIdDoesNotExist(id: Long): String = "Ученика $id не существует. Попробуйте ещё раз!"
  fun manyStudentIdsDoNotExist(ids: List<Long>): String = "Учеников с id ${ids.joinToString()} не существует. Попробуйте ещё раз!"
  fun oneIdIsGoodForStudentAddition(id: Long, courseName: String): String = "Ученик $id успешно добавлен на курс $courseName!"
  fun manyIdsAreGoodForStudentAddition(ids: List<Long>, courseName: String): String = "Ученики с id ${ids.joinToString()} успешно добавлены на курс $courseName!"

  fun oneIdAlreadyExistsForTeacherAddition(id: Long, courseName: String): String = "Преподаватель $id уже есть на курсе $courseName!"
  fun manyIdsAlreadyExistForTeacherAddition(ids: List<Long>, courseName: String): String = "Преподаватели с id ${ids.joinToString()} уже есть на курсе $courseName!"
  fun oneTeacherIdDoesNotExist(id: Long): String = "Преподавателя $id не существует. Попробуйте ещё раз!"
  fun manyTeacherIdsDoNotExist(ids: List<Long>): String = "Преподавателей с id ${ids.joinToString()} не существует. Попробуйте ещё раз!"
  fun oneIdIsGoodForTeacherAddition(id: Long, courseName: String): String = "Преподаватель $id успешно добавлен на курс $courseName!"
  fun manyIdsAreGoodForTeacherAddition(ids: List<Long>, courseName: String): String = "Преподаватели с id ${ids.joinToString()} успешно добавлены на курс $courseName!"

  fun oneIdAlreadyDoesNotExistForStudentRemoving(id: Long, courseName: String): String = "Ученика $id нет на курсе $courseName!"
  fun manyIdsAlreadyDoNotExistForStudentRemoving(ids: List<Long>, courseName: String): String = "Учеников с id ${ids.joinToString()} нет на курсе $courseName!"
  fun oneIdIsGoodForStudentRemoving(id: Long, courseName: String): String = "Ученик $id успешно удалён с курса $courseName!"
  fun manyIdsAreGoodForStudentRemoving(ids: List<Long>, courseName: String): String = "Ученики с id ${ids.joinToString()} успешно удалены с курса $courseName!"

  fun oneIdAlreadyDoesNotExistForTeacherRemoving(id: Long, courseName: String): String = "Преподавателя $id нет на курсе $courseName!"
  fun manyIdsAlreadyDoNotExistForTeacherRemoving(ids: List<Long>, courseName: String): String = "Преподавателей с id ${ids.joinToString()} нет на курсе $courseName!"
  fun oneIdIsGoodForTeacherRemoving(id: Long, courseName: String): String = "Преподаватель $id успешно удалён с курса $courseName!"
  fun manyIdsAreGoodForTeacherRemoving(ids: List<Long>, courseName: String): String = "Преподаватели с id ${ids.joinToString()} успешно удалены с курса $courseName!"

  fun menu(): String = "\u2705 Главное меню"

  fun noCoursesWasFound(): String = "Не было найдено никаких курсов!"
  fun noCoursesWasFoundForCreationOfAssignment(): String = "Сначала создайте какой-нибудь курс!"
  fun assignmentDescriptionIsNotText(): String =
    "Название серии должно являться текстом. Отправьте текстовое сообщение!"

  fun problemsDescriptionsAreNotTexts(): String =
    "Описания задач должны быть представлены в текстовом виде. Отправьте текстовое сообщение!"

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

  fun incorrectProblemDescriptionEmpty(): String =
    "Некорректный формат ввода (обнаружена пустая строка). Попробуйте ещё раз!"

  fun incorrectProblemDescriptionTooManyArguments(problemDescription: String): String =
    "Некорректный формат ввода (обнаружена строчка ($problemDescription) с более, чем 3-мя аргументами). Попробуйте ещё раз!"

  fun incorrectProblemDescriptionMaxScoreIsNotInt(maxScore: String): String =
    "Некорректный формат ввода (3-ий аргумент в какой-то строчке ($maxScore) не является числом). Попробуйте ещё раз!"

  fun assignmentWasCreatedSuccessfully(): String = "Серия успешно создана!"
}
