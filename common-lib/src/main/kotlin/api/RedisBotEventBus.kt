package com.github.heheteam.commonlib.api

import com.github.heheteam.commonlib.Problem
import com.github.heheteam.commonlib.SolutionAssessment
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub

class RedisBotEventBus(
  private val redisHost: String = "localhost",
  private val redisPort: Int = 6379,
) : BotEventBus {
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
    val simpleEvent = SimpleGradeEvent(
      studentId = studentId.id,
      chatId = chatId.long,
      messageId = messageId.long,
      grade = assessment.grade,
      comment = assessment.comment,
      problem = problem,
    )
    val event = Json.encodeToString(simpleEvent)
    jedis.publish(channel, event)
  }

  override fun subscribeToGradeEvents(handler: suspend (StudentId, RawChatId, MessageId, SolutionAssessment, Problem) -> Unit) {
    val subscriberJedis = Jedis(redisHost, redisPort)

    CoroutineScope(Dispatchers.IO).launch {
      try {
        subscriberJedis.subscribe(
          object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
              val simpleEvent = Json.decodeFromString<SimpleGradeEvent>(message)
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
    val comment: String,
    val problem: Problem,
  )
}