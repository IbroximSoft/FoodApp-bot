package uz.ibrohim.food.utils

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

//-1002561324885
fun randomQRCode(bot: Bot): Triple<String, Long, Long>? {
    val uniqueId = UUID.randomUUID().toString()
    val qrImage = generateQrCode(uniqueId)

    val (response, exception) = bot.sendPhoto(
        chatId = ChatId.fromId(-1002561324885),
        photo = TelegramFile.ByFile(qrImage),
    )

    if (exception != null) {
        println("❌ QR yuborishda exception: $exception")
        return null
    }

    val message = response?.body()?.result
    if (message == null) {
        println("❌ Message null, response: $response")
        return null
    }

    return Triple(uniqueId, message.messageId, message.chat.id)
}

fun generateQrCode(content: String): File {
    val width = 450
    val height = 450
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height)

    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = if (bitMatrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb
            bufferedImage.setRGB(x, y, color)
        }
    }

    val tempFile = File.createTempFile("qr_", ".png")
    tempFile.deleteOnExit()
    ImageIO.write(bufferedImage, "png", tempFile)
    return tempFile
}

fun askForPhoneNumber(chatId: ChatId, bot: Bot, message: String) {
    bot.sendMessage(
        chatId = chatId,
        text = message,
        replyMarkup = KeyboardReplyMarkup(
            keyboard = listOf(
                listOf(
                    KeyboardButton("📞 Telefon raqamni yuborish", requestContact = true)
                )
            ),
            resizeKeyboard = true,
            oneTimeKeyboard = true
        )
    )
}