package com.github.heheteam.studentbot.state.quiz

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.LocalMediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.interfaces.StudentId
import com.github.heheteam.commonlib.util.NewState
import com.github.heheteam.commonlib.util.Unhandled
import com.github.heheteam.studentbot.state.MenuState
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.utils.buildEntities

class L1S0(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        send(
            "\uD83C\uDF32 Ты входишь в Числовой Лес. " +
                    "Всё здесь построено из чисел: деревья считают листья, кусты шепчут примеры."
        )

        val buttons = listOf("\uD83D\uDE80 Да, откроем ворота!", "\uD83D\uDD19 Назад")
        send(
            "\uD83D\uDC36 Дуся: \"Вместе мы пройдём сквозь чащу и найдём тайный выход! " +
                    "Но сначала — откроем ворота. Только тот, кто решит задачу, может пройти дальше.\"\n",
            replyMarkup = horizontalKeyboard(buttons),
        )
            .also { messagesWithKeyboard.add(it) }

        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(L1S1(context, userId))
                buttons[1] -> NewState(MenuState(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S1(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        val firstNumber = 17
        val secondNumber = 5
        send("Введи правильную сумму: $firstNumber + $secondNumber")

        addTextMessageHandler { message ->
            when (message.content.text.trim().toIntOrNull()) {
                null -> {
                    send("Надо ввести число")
                    NewState(L1S1(context, userId))
                }

                firstNumber + secondNumber -> NewState(L1S2(context, userId))
                else -> {
                    NewState(L1S1Wrong(context, userId))
                }
            }
        }
    }
}

class L1S1Wrong(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        val buttons = listOf("✅ Конечно!", "\uD83D\uDD19 Назад")
        send(
            "\uD83D\uDD12 Ворота задрожали… но остались закрыты. " +
                    "Наверное, ответ был неверный. Деревья недовольно зашумели.\n Попробуем ещё раз? ",
            replyMarkup = horizontalKeyboard(buttons),
        )
            .also { messagesWithKeyboard.add(it) }
        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(L1S1(context, userId))
                buttons[1] -> NewState(MenuState(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S2(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        send(
            "\uD83C\uDF0A Перед тобой — речка. " +
                    "Через неё можно перебраться только по камням, лежащим в нужном порядке."
        )
        val buttons =
            listOf(
                "\uD83C\uDFDE Перейти к речке",
                "\uD83E\uDD17 Почесать пузо Дусе",
                "\uD83D\uDD19  Назад",
            )
        send(
            "\uD83D\uDC36 Дуся: \"Кстати... хочешь почесать мне пузо? Я это очень люблю!\"\n",
            replyMarkup = horizontalKeyboard(buttons),
        )
            .also { messagesWithKeyboard.add(it) }
        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(L1S3(context, userId))
                buttons[1] -> NewState(L1S3Bellyrub(context, userId))
                buttons[1] -> NewState(MenuState(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S3(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        send("\uD83E\uDEA8 Камни выстроились в линию, но один из них исчез…\n" + "2 → 4 → 8 → 16 → ❓")
        val buttons = listOf("\uD83C\uDFDE Перескочить речку", "\uD83D\uDD19  Назад")
        send(
            "\uD83D\uDC36 Дуся: \"Ты справишься! Главное — не оступиться!\"",
            replyMarkup = horizontalKeyboard(buttons),
        )
            .also { messagesWithKeyboard.add(it) }
        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(L1S31(context, userId))
                buttons[1] -> NewState(MenuState(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S3Bellyrub(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        val content = TextWithMediaAttachments(
            attachments = listOf(LocalMediaAttachment(AttachmentKind.DOCUMENT, "/maze_correct.mp4")),
        )
        val buttons = listOf("\uD83C\uDFDE Перейти к речке")
        send(content)
        send(
            "Мррр... \uD83D\uDE0C Спасибо! Это было чудесно. " +
                    "Теперь у меня ещё больше сил пройти речку вместе с тобой!",
            replyMarkup = horizontalKeyboard(buttons)
        ).also { messagesWithKeyboard.add(it) }
        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(L1S31(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S3Wrong(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        val buttons = listOf("✅ Конечно!", "\uD83D\uDD19 Назад")
        send(
            "*Бульк!* — чуть не оступился! " +
                    "Цепочка чисел была неправильная — надо попробовать ещё раз, чтобы не промокнуть!\n" +
                    "Попробуем ещё раз?",
            replyMarkup = horizontalKeyboard(buttons),
        )
            .also { messagesWithKeyboard.add(it) }
        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(L1S31(context, userId))
                buttons[1] -> NewState(MenuState(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S4(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        send("Ты перепрыгнул речку! \uD83C\uDF89 Вас встречает огромное говорящее дерево.\n")
        val buttons =
            listOf("\uD83E\uDDE0 Подойти к дубу", "\uD83E\uDD17 Почесать ещё раз", "\uD83D\uDD19  Назад")
        send(
            "\uD83D\uDC36 Дуся: \"А пока ты отдыхаешь, может, почешешь мне пузо? Ну пожааалуйста! \uD83E\uDD7A\"\n",
            replyMarkup = horizontalKeyboard(buttons),
        )
            .also { messagesWithKeyboard.add(it) }
        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(MenuState(context, userId)) // TODO
                buttons[1] -> NewState(L1S4Bellyrub(context, userId))
                buttons[2] -> NewState(MenuState(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S4Bellyrub(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        val buttons = listOf("\uD83C\uDFDE Подойти к дубу")
        send(
            "Ах, да! Ты — самый лучший пузочесатель на свете! \uD83D\uDC3E",
            replyMarkup = horizontalKeyboard(buttons),
        )
            .also { messagesWithKeyboard.add(it) }
        addDataCallbackHandler { callbackQuery ->
            when (callbackQuery.data) {
                buttons[0] -> NewState(L1S31(context, userId))
                else -> Unhandled
            }
        }
    }
}

class L1S31(override val context: User, override val userId: StudentId) : QuestState() {
    override suspend fun BotContext.run() {
        send("\uD83E\uDEA8 Введи недостающее значение\n" + "2 → 4 → 8 → 16 → ❓")
        val correctAnswer = 32
        addTextMessageHandler { message ->
            when (message.content.text.trim().toIntOrNull()) {
                null -> {
                    send("Надо ввести число")
                    NewState(L1S1(context, userId))
                }

                correctAnswer -> NewState(L1S4(context, userId))
                else -> {
                    NewState(L1S3Wrong(context, userId))
                }
            }
        }
    }
}
