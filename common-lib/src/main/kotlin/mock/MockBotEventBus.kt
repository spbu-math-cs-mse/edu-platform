package com.github.heheteam.commonlib.mock

import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.api.BotEventBus
import com.github.heheteam.commonlib.api.ProblemId
import com.github.heheteam.commonlib.api.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId

class MockBotEventBus : BotEventBus {
    override suspend fun publishGradeEvent(
        studentId: StudentId,
        chatId: RawChatId,
        messageId: MessageId,
        assessment: SolutionAssessment,
        problemId: ProblemId
    ) {
        // Do nothing
    }

    override fun subscribeToGradeEvents(handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, ProblemId) -> Unit) {
        // Do nothing
    }
} 