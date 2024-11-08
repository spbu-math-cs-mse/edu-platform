package states

import Dialogues.noSolutionsToCheck
import Dialogues.solutionInfo
import Keyboards
import SolutionType
import com.github.heheteam.samplebot.mockSolutions
import com.github.heheteam.samplebot.mockTeachers
import dev.inmo.tgbotapi.extensions.api.delete
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf

@OptIn(RiskFeature::class, ExperimentalCoroutinesApi::class)
fun DefaultBehaviourContextWithFSM<BotState>.strictlyOnGettingSolutionState() {
  strictlyOn<GettingSolutionState> { state ->
    if (state.context.username == null) {
      return@strictlyOn null
    }
    val username = state.context.username!!.username
    if (!mockTeachers.containsKey(username)) {
      return@strictlyOn StartState(state.context)
    }

    if (mockSolutions.isEmpty()) {
      bot.send(
        state.context,
        noSolutionsToCheck(),
      )
    } else {
      val solution = mockSolutions.random()
      val getSolution: ContentMessage<*>
      var getMarkup: ContentMessage<*>? = null
      when (solution.type) {
        SolutionType.TEXT -> getSolution = bot.send(
          state.context,
          solution.content.text!! + "\n\n\n" + solutionInfo(solution),
          replyMarkup = Keyboards.solutionMenu(),
        )

        SolutionType.PHOTO -> getSolution = bot.sendPhoto(
          state.context,
          InputFile.fromId(solution.content.fileIds!![0]),
          text = if (solution.content.text == null) solutionInfo(solution) else solution.content.text + "\n\n\n" + solutionInfo(solution),
          replyMarkup = Keyboards.solutionMenu(),
        )

        SolutionType.PHOTOS -> {
          getSolution = bot.sendMediaGroup(state.context, listOf(TelegramMediaPhoto(InputFile.fromId(solution.content.fileIds!![0]), solution.content.text)) + solution.content.fileIds!!.map { TelegramMediaPhoto(InputFile.fromId(it)) }.drop(1))
          getMarkup = bot.send(state.context, solutionInfo(solution), replyMarkup = Keyboards.solutionMenu())
        }

        SolutionType.DOCUMENT -> getSolution = bot.sendDocument(
          state.context,
          InputFile.fromId(solution.content.fileIds!![0]),
          text = if (solution.content.text == null) solutionInfo(solution) else solution.content.text + "\n\n\n" + solutionInfo(solution),
          replyMarkup = Keyboards.solutionMenu(),
        )
      }

      when (val response = flowOf(waitDataCallbackQuery(), waitTextMessage()).flattenMerge().first()) {
        is DataCallbackQuery -> {
          val command = response.data
          if (command == Keyboards.returnBack) {
            delete(getSolution)
            if (getMarkup != null) {
              delete(getMarkup)
            }
          }
        }
      }
    }
    MenuState(state.context)
  }
}
