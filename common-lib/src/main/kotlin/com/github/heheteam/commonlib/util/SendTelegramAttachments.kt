package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.SolutionAttachment
import com.github.heheteam.commonlib.SolutionContent
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
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
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
suspend fun BehaviourContext.sendSolutionContent(
  chatId: ChatId,
  solutionContent: SolutionContent,
  replyMarkup: InlineKeyboardMarkup? = null,
): ContentMessage<*> {
  val attachments = solutionContent.attachments
  val text = solutionContent.text
  val singleAttachment = attachments.singleOrNull()
  return if (attachments.isEmpty()) {
    sendMessage(chatId, text, replyMarkup = replyMarkup)
  } else if (singleAttachment != null) {
    sendSingleMedia(singleAttachment, chatId, solutionContent)
  } else {
    sendMediaGroup(attachments, text, chatId)
  }
}

private suspend fun BehaviourContext.sendSingleMedia(
  singleAttachment: SolutionAttachment,
  chatId: ChatId,
  solutionContent: SolutionContent,
): ContentMessage<MediaGroupPartContent> {
  val file = downloadFile(singleAttachment.downloadUrl, singleAttachment.uniqueString)
  return when (singleAttachment.kind) {
    AttachmentKind.PHOTO -> {
      sendPhoto(chatId, file.asMultipartFile(), text = solutionContent.text)
    }
    AttachmentKind.DOCUMENT -> {
      sendDocument(chatId, file.asMultipartFile(), text = solutionContent.text)
    }
  }.also {
    try {
      file.delete()
    } catch (_: Throwable) {}
  }
}

@OptIn(RiskFeature::class)
private suspend fun BehaviourContext.sendMediaGroup(
  attachments: List<SolutionAttachment>,
  text: String,
  chatId: ChatId,
): ContentMessage<MediaGroupContent<MediaGroupPartContent>> {
  val (telegramMediaGroup, files) =
    attachments
      .mapIndexed { index, media ->
        val firstMediaText = if (index == 0) text else null
        val file = downloadFile(media.downloadUrl, media.uniqueString)
        when (media.kind) {
          AttachmentKind.PHOTO -> {
            TelegramMediaPhoto(file.asMultipartFile(), firstMediaText)
          }

          AttachmentKind.DOCUMENT -> {
            TelegramMediaDocument(file.asMultipartFile(), firstMediaText)
          }
        } to file
      }
      .unzip()
  return bot.sendMediaGroup(chatId, telegramMediaGroup).also {
    files.forEach { file ->
      try {
        file.delete()
      } catch (_: Throwable) {}
    }
  }
}
