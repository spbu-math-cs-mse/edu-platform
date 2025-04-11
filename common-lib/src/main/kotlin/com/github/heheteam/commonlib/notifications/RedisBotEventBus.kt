package com.github.heheteam.commonlib.notifications

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.Solution
import com.github.heheteam.commonlib.SolutionAssessment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import com.github.heheteam.commonlib.interfaces.StudentId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub

class RedisBotEventBus(private val redisHost: String, private val redisPort: Int) : BotEventBus {
  private val jedis = Jedis(redisHost, redisPort)
  private val channel = "grade_events"

  init {
    println("Connecting to Redis at $redisHost:$redisPort")
  }

  override fun publishGradeEvent(
    studentId: StudentId,
    chatId: RawChatId,
    messageId: MessageId,
    assessment: SolutionAssessment,
    problem: Problem,
  ) {
    val simpleEvent =
      SimpleGradeEvent(
        studentId = studentId.long,
        chatId = chatId.long,
        messageId = messageId.long,
        grade = assessment.grade,
        comment = assessment.comment,
        problem = problem,
      )
    val event = Json.encodeToString(simpleEvent)
    jedis.publish(channel, event)
  }

  override fun publishNewSolutionEvent(solution: Solution) {
    val simpleEvent = NewSolutionEvent(solution)
    val event = Json.encodeToString(simpleEvent)
    jedis.publish(channel, event)
  }

  override fun subscribeToNewSolutionEvent(handler: suspend (Solution) -> Unit) {
    val subscriberJedis = Jedis(redisHost, redisPort)

    CoroutineScope(Dispatchers.IO).launch {
      try {
        subscriberJedis.subscribe(
          object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
              val simpleEvent =
                kotlin.runCatching { Json.decodeFromString<NewSolutionEvent>(message) }.getOrNull()
                  ?: return
              runBlocking { handler(simpleEvent.solution) }
            }
          },
          channel,
        )
      } catch (e: Exception) {
        println("Error in Redis subscription: ${e.message}")
        e.printStackTrace()
      }
    }
  }

  override fun subscribeToGradeEvents(
    handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit
  ) {
    val subscriberJedis = Jedis(redisHost, redisPort)

    CoroutineScope(Dispatchers.IO).launch {
      try {
        subscriberJedis.subscribe(
          object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
              val simpleEvent =
                kotlin.runCatching { Json.decodeFromString<SimpleGradeEvent>(message) }.getOrNull()
                  ?: return@onMessage
              runBlocking {
                handler(
                  StudentId(simpleEvent.studentId),
                  RawChatId(simpleEvent.chatId),
                  MessageId(simpleEvent.messageId),
                  SolutionAssessment(simpleEvent.grade, simpleEvent.comment),
                  simpleEvent.problem,
                )
              }
            }
          },
          channel,
        )
      } catch (e: Exception) {
        println("Error in Redis subscription: ${e.message}")
        e.printStackTrace()
      }
    }
  }

  @Serializable
  private data class SimpleGradeEvent(
    val studentId: Long,
    val chatId: Long,
    val messageId: Long,
    val grade: Int,
    val comment: TextWithMediaAttachments,
    val problem: Problem,
  )

  @Serializable private data class NewSolutionEvent(val solution: Solution)
}
