package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.TelegramAttachment
import com.github.heheteam.commonlib.TelegramMediaKind
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.utils.RiskFeature
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels

fun downloadFile(fileURL: String, outputFileName: String): File {
  val extension = fileURL.substringAfterLast(".")
  val url: URL = URI(fileURL).toURL()
  val file = File("$outputFileName.$extension")
  url.openStream().use {
    Channels.newChannel(it).use { rbc ->
      FileOutputStream(file).use { fos -> fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE) }
    }
  }
  return file
}

@OptIn(RiskFeature::class)
suspend fun BehaviourContext.sendAttachments(
  chatId: ChatId,
  attachment: TelegramAttachment,
  replyMarkup: InlineKeyboardMarkup? = null,
): ContentMessage<*> {
  val mediaUnits = attachment.media
  val text = attachment.text
  val singleMedia = mediaUnits.singleOrNull()
  return if (mediaUnits.isEmpty()) {
    sendMessage(chatId, text, replyMarkup = replyMarkup)
  } else if (singleMedia != null) {

    val file = downloadFile(singleMedia.downloadUrl, singleMedia.uniqueString)
    return when (singleMedia.kind) {
      TelegramMediaKind.PHOTO -> {
        sendPhoto(chatId, file.asMultipartFile(), text = attachment.text)
      }
      TelegramMediaKind.DOCUMENT -> {
        sendDocument(chatId, file.asMultipartFile(), text = attachment.text)
      }
    }
  } else {
    val telegramData =
      mediaUnits.mapIndexed { index, media ->
        val firstMediaText = if (index == 0) text else null
        val file = downloadFile(media.downloadUrl, media.uniqueString)
        when (media.kind) {
          TelegramMediaKind.PHOTO -> {
            TelegramMediaPhoto(file.asMultipartFile(), firstMediaText)
          }
          TelegramMediaKind.DOCUMENT -> {
            TelegramMediaDocument(file.asMultipartFile(), firstMediaText)
          }
        }
      }
    bot.sendMediaGroup(chatId, telegramData)
  }
}
