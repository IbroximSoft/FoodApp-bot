package uz.ibrohim.food

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import uz.ibrohim.food.state.State
import uz.ibrohim.food.state.Step
import uz.ibrohim.food.utils.askForPhoneNumber

fun main() {
    println("Salom! Dastur test rejimida ishga tushdi!") 
    /*
    FirebaseService() // TEST UCHUN
    val bot = bot { ... }
    bot.startPolling()
    */
}

private val userState = mutableMapOf<Long, State>()

fun handleContactReception(dispatcher: Dispatcher) {
    dispatcher.command("start") {
        val chatId = message.chat.id
        userState[chatId] = State(step = Step.ASK_NUMBER)

        askForPhoneNumber(ChatId.fromId(chatId), bot, "📲 Iltimos, telefon raqamingizni yuboring.")
    }

    dispatcher.contact {
        val chatId = message.chat.id
        val contact = message.contact
        val fromUser = message.from ?: return@contact

        UserHandler(bot).handler(
            chatId = chatId,
            userState = userState,
            contact = contact!!,
            fromUser = fromUser
        )
    }

    dispatcher.message {
        val chatId = message.chat.id
        val currentState = userState[chatId]

        if (message.text == "/start") return@message

        if (currentState?.step == Step.ASK_NUMBER && message.contact == null) {
            askForPhoneNumber(ChatId.fromId(chatId), bot, "📲 Quyidagi tugma orqali telefon raqamingizni yuboring.")
        }

        when (message.text) {
            "🎫 QR-Kodim" -> {
                val handler = UserHandler(bot)
                val phoneNumber = handler.getPhoneNumberFromCache(message.from!!.id.toString()) // mana bu qo‘shimcha
                if (phoneNumber != null) {
                    val cachedUser = handler.getCachedUser(phoneNumber)
                    if (cachedUser != null) {
                        bot.sendPhoto(
                            chatId = ChatId.fromId(message.chat.id),
                            photo = TelegramFile.ByFileId(cachedUser.fileId),
                            caption = "Sizning QR-Kodingiz"
                        )
                        return@message
                    }
                }
                bot.sendMessage(ChatId.fromId(message.chat.id), "❌ QR kod topilmadi.")
            }

            "🍽 Menyu (Taomlar)" -> {
                bot.sendPhoto(
                    chatId = ChatId.fromId(message.chat.id),
                    photo = TelegramFile.ByFileId("AgACAgIAAyEFAASYqrdVAAMUaCNuauHIS4rkKQpfEuzVwKi0RDIAAuntMRvk5xlJ0FMGxeNuyycBAAMCAAN5AAM2BA")
                )
            }

            "⏰ Ish vaqti" -> {
                UserHandler(bot).checkCafeStatus(ChatId.fromId(chatId))
            }

            "ℹ️ Kafe haqida" -> {
                UserHandler(bot).cafeAbout(ChatId.fromId(chatId))
            }

            "🛍 Mening xaridlarim" -> {
                val userId = UserHandler(bot).getUniqueIdFromCache(ChatId.fromId(chatId))
                if (userId != null) {
                    UserHandler(bot).tradeHistory(bot, ChatId.fromId(chatId), userId)
                }
            }
        }
    }

    dispatcher.callbackQuery {
        val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
        val data = callbackQuery.data

        if (data.startsWith("SHOW_LOCATION")) {
            val parts = data.split(":")
            if (parts.size == 3) {
                val lat = parts[1].toDoubleOrNull()
                val lon = parts[2].toDoubleOrNull()

                if (lat != null && lon != null) {
                    bot.sendLocation(
                        chatId = ChatId.fromId(chatId),
                        latitude = lat.toFloat(),
                        longitude = lon.toFloat()
                    )
                } else {
                    bot.sendMessage(ChatId.fromId(chatId), "⚠️ Lokatsiya noto‘g‘ri.")
                }
            }
        }
    }

    dispatcher.photos {
        val fileId = message.photo?.lastOrNull()?.fileId
        println("Rasm file_id: $fileId")
    }
}
