import com.github.heheteam.commonlib.Student
import dev.inmo.tgbotapi.requests.abstracts.InputFile

object Dialogues {
  val okSticker = InputFile.fromId("CAACAgEAAxkBAAIBJWcUPyqe-UEVGqMmhNYi21U3gkBEAALrAQACOA6CEbOGBM7hnEk5NgQ")
  val typingSticker = InputFile.fromId("CAACAgEAAxkBAAIBb2cdPy6r60MpNFXFLAABaQWOqfgCHAAC6QADZszART3es5n3X_q7NgQ")
  val greetingSticker = InputFile.fromId("CAACAgEAAxkBAAIBbmcdPydqt93f8S1XKHV3z73nUoLgAALxAQACOA6CEXTVKqzkcGAkNgQ")
  val heartSticker = InputFile.fromId("CAACAgEAAxkBAAIBqGcePu_DPJQIwyU2hfH7SbMJ1g_DAAL6AQACOA6CEZdviQ02NivYNgQ")
  val nerdSticker = InputFile.fromId("CAACAgEAAxkBAAIBxGceRcgL_nCWjiRrMzWQSi_MdlDzAAL4AQACOA6CEVv05nratTJkNgQ")

  fun greetings(): String =
    "Привет! Меня зовут такса Дуся, и я отлично разбираюсь в олимпиадной математике.\n" +
      "Мои охотничьи инстинкты помогают мне находить классные олимпиадные задачи, " +
      "а превосходный нюх - отыскивать ошибки в твоих рассуждениях, гав-гав!\n\n" +
      "Свистни мне, когда захочешь порешать з-аф-аф-дачи! Также я помогу тебе не пропустить интересные" +
      " математические события - я тщательно за ними слежу!\n\n"

  fun devAskForId(): String = "Введите свой id:"
  fun devIdNotFound(): String = "Этот id не был найден в базе данных! Попробуйте ещё раз:"

  fun askFirstName(): String = "Как я могу к тебе обращаться? Напиши свое имя."

  fun askLastName(firstName: String): String = "Отлично, $firstName, введи свою фамилию \uD83D\uDC47"

  fun askGrade(
    firstName: String,
    lastName: String,
  ): String = "Безумно рада нашему с тобой знакомству, $firstName $lastName!\nВ каком классе ты учишься?"

  fun menu(): String = "\u2705 Главное меню"

  fun petDog(): String =
    "Гав-гав!\n\n" +
      "Аф-аф! Ты настоящий товарищ и друг всех зверей! Ты так хорошо и много чесал меня, что " +
      "стал настоящим профессионалом в этом. Дабы мотивировать тебя на дальнейшее изучение " +
      "таксочесательных наук, я дарю тебе 1000 таксо-баллов, которые ты можешь потратить на " +
      "покупку курсов Дабромат! Для использования этой возможности свяжись с администратором " +
      "@dabromat"

  fun giveFeedback(): String =
    "Здесь ты можешь написать все свои мысли по поводу курсов, " +
      "а я передам это администраторам"

  fun acceptFeedback(): String = "Отлично, я уже побежал передавать твой ответ администратору"

  fun childPerformance(
    child: Student,
    core: ParentCore,
  ) = "Успеваемость ребенка $child:\n\n" +
    (
      core
        .getStudentPerformance(child.id)
        .map { "Задача ${it.key} решена на ${it.value} баллов" }
        .joinToString("\n")
      )
}
