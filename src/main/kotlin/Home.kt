package uz.ibrohim.food

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.types.TelegramBotResult
import uz.ibrohim.food.state.State
import uz.ibrohim.food.state.Step
import uz.ibrohim.food.utils.askForPhoneNumber

fun main() {
    FirebaseService()
    val bot = bot {
        token = "5774652487:AAFPup9Dg-ihLoIZOAI564wgoJOc1IR30sc"

        dispatch {
            handleContactReception(this)
        }
    }

    bot.startPolling()
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
                val fromId = message.from!!.id.toString()
                var phoneNumber = handler.getPhoneNumberFromCache(fromId)

                // ⏳ 1. "Yuklanmoqda" xabarini chiqaramiz
                val loadingResponse = bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "⏳ QR kod yuklanmoqda..."
                )

                val loadingMessageId = if (loadingResponse is TelegramBotResult.Success) {
                    loadingResponse.value.messageId
                } else {
                    null
                }

                if (phoneNumber == null) {
                    val userFromFirebase = handler.getUserByChatId(fromId)
                    if (userFromFirebase != null) {
                        phoneNumber = userFromFirebase.number
                        handler.saveToCache(phoneNumber, userFromFirebase)
                    }
                }

                if (phoneNumber != null) {
                    var cachedUser = handler.getCachedUser(phoneNumber)

                    if (cachedUser == null) {
                        cachedUser = handler.getUserByNumber(phoneNumber)
                        if (cachedUser != null) {
                            handler.saveToCache(phoneNumber, cachedUser)
                        }
                    }

                    if (cachedUser != null) {
                        try {
                            val copyResult = bot.copyMessage(
                                fromChatId = ChatId.fromId(cachedUser.fromChatId.toLong()),
                                chatId = ChatId.fromId(chatId),
                                messageId = cachedUser.messageId.toLong(),
                                caption = "Sizning QR kodingiz"
                            )

                            val exception = copyResult.second
                            if (exception != null) {
                                println("❌ copyMessage error (handled): $exception")
                            }

                            // ✅ Yuklanmoqda xabarini o‘chirish
                            if (loadingMessageId != null) {
                                bot.deleteMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    messageId = loadingMessageId
                                )
                            }

                            return@message
                        } catch (e: Exception) {
                            println("❌ Exception while copying message: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }

                // ❌ Topilmasa
                if (loadingMessageId != null) {
                    bot.deleteMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        messageId = loadingMessageId
                    )
                }
                bot.sendMessage(ChatId.fromId(chatId), "❌ QR kod topilmadi.")
            }

            "🍽 Menyu (Taomlar)" -> {
                bot.copyMessage(
                    fromChatId = ChatId.fromId(-1002561324885),
                    chatId = ChatId.fromId(chatId),
                    messageId = 28
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
