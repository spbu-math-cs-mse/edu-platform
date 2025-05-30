package com.github.heheteam.commonlib.util

import com.github.heheteam.commonlib.AttachmentKind
import com.github.heheteam.commonlib.MediaAttachment
import com.github.heheteam.commonlib.TextWithMediaAttachments
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.get.getFileAdditionalInfo
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.media.sendMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.reply
import dev.inmo.tgbotapi.extensions.api.send.replyWithDocument
import dev.inmo.tgbotapi.extensions.api.send.replyWithMediaGroup
import dev.inmo.tgbotapi.extensions.api.send.replyWithPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup
import dev.inmo.tgbotapi.types.media.TelegramMediaDocument
import dev.inmo.tgbotapi.types.media.TelegramMediaPhoto
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.AudioContent
import dev.inmo.tgbotapi.types.message.content.DocumentContent
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
import dev.inmo.tgbotapi.types.message.content.PhotoContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.message.content.VideoContent
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

suspend fun TelegramBot.sendTextWithMediaAttachments(
  chatId: ChatId,
  content: TextWithMediaAttachments,
  replyMarkup: InlineKeyboardMarkup? = null,
  replyTo: MessageId? = null,
): ContentMessage<*> {
  val attachments = content.attachments
  val text = content.text
  val singleAttachment = attachments.singleOrNull()
  return if (attachments.isEmpty()) {
    if (replyTo == null) {
      sendMessage(chatId, text, replyMarkup = replyMarkup)
    } else {
      reply(chatId, replyTo, text, replyMarkup = replyMarkup)
    }
  } else if (singleAttachment != null) {
    sendSingleMedia(singleAttachment, chatId, content, replyTo)
  } else {
    sendSubmissionAsGroupMedia(attachments, text, chatId, replyTo)
  }
}

private suspend fun TelegramBot.sendSingleMedia(
  singleAttachment: MediaAttachment,
  chatId: ChatId,
  submissionContent: TextWithMediaAttachments,
  replyTo: MessageId? = null,
): ContentMessage<MediaGroupPartContent> {
  val file = downloadFile(singleAttachment.downloadUrl, singleAttachment.uniqueString)
  return when (singleAttachment.kind) {
    AttachmentKind.PHOTO -> {
      if (replyTo == null) {
        sendPhoto(chatId, file.asMultipartFile(), text = submissionContent.text)
      } else {
        replyWithPhoto(chatId, replyTo, file.asMultipartFile(), text = submissionContent.text)
      }
    }
    AttachmentKind.DOCUMENT -> {
      if (replyTo == null) {
        sendDocument(chatId, file.asMultipartFile(), text = submissionContent.text)
      } else {
        replyWithDocument(chatId, replyTo, file.asMultipartFile(), text = submissionContent.text)
      }
    }
  }.also {
    try {
      file.delete()
    } catch (e: SecurityException) {
      KSLog.error("Failed to delete file after sending single media: ${e.message}")
    }
  }
}

@OptIn(RiskFeature::class)
private suspend fun TelegramBot.sendSubmissionAsGroupMedia(
  attachments: List<MediaAttachment>,
  text: String,
  chatId: ChatId,
  replyTo: MessageId? = null,
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
  val message =
    if (replyTo == null) sendMediaGroup(chatId, telegramMediaGroup)
    else replyWithMediaGroup(chatId, replyTo, telegramMediaGroup)
  return message.also {
    files.forEach { file ->
      try {
        file.delete()
      } catch (e: SecurityException) {
        KSLog.error("Failed to delete file after sending media group", e)
      }
    }
  }
}

private suspend fun makeURL(content: MediaContent, botToken: String, bot: TelegramBot): String {
  val contentInfo = bot.getFileAdditionalInfo(content)
  return "https://api.telegram.org/file/bot$botToken/${contentInfo.filePath}"
}

suspend fun extractTextWithMediaAttachments(
  message: CommonMessage<*>,
  botToken: String,
  bot: TelegramBot,
): TextWithMediaAttachments? =
  when (val content = message.content) {
    is TextContent -> TextWithMediaAttachments(content.text)
    is PhotoContent ->
      extractSingleAttachment(content.text.orEmpty(), AttachmentKind.PHOTO, content, botToken, bot)

    is DocumentContent ->
      extractSingleAttachment(
        content.text.orEmpty(),
        AttachmentKind.DOCUMENT,
        content,
        botToken,
        bot,
      )

    is MediaGroupContent<*> -> extractMultipleAttachments(content, botToken, bot)
    else -> null
  }

private suspend fun extractSingleAttachment(
  text: String,
  attachmentKind: AttachmentKind,
  content: MediaContent,
  botToken: String,
  bot: TelegramBot,
) =
  TextWithMediaAttachments(
    text,
    listOf(
      MediaAttachment(attachmentKind, makeURL(content, botToken, bot), content.media.fileId.fileId)
    ),
  )

private suspend fun extractMultipleAttachments(
  content: MediaGroupContent<*>,
  botToken: String,
  bot: TelegramBot,
): TextWithMediaAttachments? =
  TextWithMediaAttachments(
    content.text.orEmpty(),
    content.group.map {
      val kind =
        when (it.content) {
          is DocumentContent -> AttachmentKind.DOCUMENT
          is PhotoContent -> AttachmentKind.PHOTO
          is AudioContent -> return null
          is VideoContent -> return null
        }
      MediaAttachment(kind, makeURL(it.content, botToken, bot), it.content.media.fileId.fileId)
    },
  )
