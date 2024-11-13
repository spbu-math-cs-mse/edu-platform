package states

import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import kotlinx.coroutines.ExperimentalCoroutinesApi
// import Dialogues
// import Dialogues.solutionNotSent
// import Dialogues.solutionSent
// import Keyboards
// import Problem
// import Solution
// import SolutionContent
// import com.github.heheteam.samplebot.mockSolutions
// import com.github.heheteam.samplebot.mockTeachers
// import dev.inmo.tgbotapi.extensions.api.delete
// import dev.inmo.tgbotapi.extensions.api.send.media.sendSticker
// import dev.inmo.tgbotapi.extensions.api.send.send
// import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.*
// import dev.inmo.tgbotapi.extensions.utils.documentContentOrNull
// import dev.inmo.tgbotapi.extensions.utils.mediaGroupContentOrNull
// import dev.inmo.tgbotapi.extensions.utils.photoContentOrNull
// import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
// import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
// import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
// import kotlinx.coroutines.flow.first
// import kotlinx.coroutines.flow.flattenMerge
// import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnTestSendingSolutionState() {
//  strictlyOn<TestSendingSolutionState> { state ->
//    if (state.context.username == null) {
//      return@strictlyOn null
//    }
//    val username = state.context.username!!.username
//    if (!mockTeachers.containsKey(username)) {
//      return@strictlyOn StartState(state.context)
//    }
//
//    val testSendMessage =
//      bot.send(
//        state.context,
//        Dialogues.testSendSolution(),
//        replyMarkup = Keyboards.returnBack(),
//      )
//
//    when (
//      val response =
//        flowOf(waitDataCallbackQuery(), waitTextMessage(), waitMediaMessage(), waitDocumentMessage()).flattenMerge().first()
//    ) {
//      is DataCallbackQuery -> {
//        val command = response.data
//        if (command == Keyboards.returnBack) {
//          delete(testSendMessage)
//        }
//      }
//
//      is CommonMessage<*> -> {
//        val textSolution = response.content.textContentOrNull()
//        val photoSolution = response.content.photoContentOrNull()
//        val photosSolution = response.content.mediaGroupContentOrNull()?.group?.map { it.content.photoContentOrNull() }
//        val documentSolution = response.content.documentContentOrNull()
//
//        if (textSolution != null || photoSolution != null || photosSolution != null || documentSolution != null) {
//          if (textSolution != null) {
//            mockSolutions.add(
//              Solution(
//                (mockSolutions.size + 1).toString(),
//                Problem(""),
//                SolutionContent(text = textSolution.text),
//                SolutionType.TEXT,
//              ),
//            )
//          } else if (photoSolution != null) {
//            mockSolutions.add(
//              Solution(
//                (mockSolutions.size + 1).toString(),
//                Problem(""),
//                SolutionContent(listOf(photoSolution.media.fileId.fileId), photoSolution.text),
//                SolutionType.PHOTO,
//              ),
//            )
//          } else if (photosSolution != null) {
//            mockSolutions.add(
//              Solution(
//                (mockSolutions.size + 1).toString(),
//                Problem(""),
//                SolutionContent(photosSolution.map { it!!.media.fileId.fileId }, photosSolution[0]!!.text),
//                SolutionType.PHOTOS,
//              ),
//            )
//          } else {
//            mockSolutions.add(
//              Solution(
//                (mockSolutions.size + 1).toString(),
//                Problem(""),
//                SolutionContent(listOf(documentSolution!!.media.fileId.fileId), text = documentSolution.text),
//                SolutionType.DOCUMENT,
//              ),
//            )
//          }
//          bot.sendSticker(
//            state.context,
//            Dialogues.okSticker,
//          )
//          bot.send(
//            state.context,
//            solutionSent(),
//          )
//        } else {
//          bot.send(
//            state.context,
//            solutionNotSent(),
//          )
//        }
//      }
//    }
//    MenuState(state.context)
//  }
}
