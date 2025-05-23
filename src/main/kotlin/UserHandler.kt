package uz.ibrohim.food

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.ibrohim.food.data.UserData
import uz.ibrohim.food.state.State
import uz.ibrohim.food.state.Step
import uz.ibrohim.food.utils.UserCacheManager
import uz.ibrohim.food.utils.randomQRCode
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class UserHandler(private val bot: Bot) {

    fun getCachedUser(phone: String): UserData? {
        val cached = UserCacheManager.getUser(phone)
        return cached
    }

    fun saveToCache(key: String, user: UserData) {
        UserCacheManager.saveUser(key, user)
    }

    fun handler(chatId: Long, userState: MutableMap<Long, State>, contact: Contact, fromUser: User) {
        // 1. Foydalanuvchiga menyuni ko‘rsatamiz (darhol)
        userState[chatId] = State(step = Step.ASK_MENU)
        showMainMenu(ChatId.fromId(chatId))

        // 2. Fon rejimida tekshirish va saqlash
        CoroutineScope(Dispatchers.IO).launch {
            val phone = contact.phoneNumber.replace("+", "")
            val cached = UserCacheManager.getUser(phone)
            if (cached != null) {
                return@launch
            }

            // Firebase'dan tekshirish
            val existingUser = getUserByNumber(phone)
            if (existingUser != null) {
                saveToCache(phone, existingUser)

            } else {
                // Yangi foydalanuvchi - QR kod yaratish
                val qrInfo = randomQRCode(bot)
                if (qrInfo != null) {
                    val (uniqueId, fileId, fromChatId) = qrInfo
                    val newUser = UserData(
                        name = fromUser.firstName,
                        number = phone,
                        chatId = fromUser.id.toString(),
                        uniqueId = uniqueId,
                        messageId = fileId.toString(),
                        fromChatId = fromChatId.toString() // ✅ To‘g‘rilash kerak
                    )

                    // Firebase'ga saqlash
                    saveUserToDatabase(newUser)

                    // Cache'ga saqlash
                    saveToCache(phone, newUser)
                }
            }
        }
    }

    private fun showMainMenu(chatId: ChatId) {

        val keyboard = KeyboardReplyMarkup(
            keyboard = listOf(
                listOf(KeyboardButton("🎫 QR-Kodim")),
                listOf(KeyboardButton("🍽 Menyu (Taomlar)"), KeyboardButton("⏰ Ish vaqti")),
                listOf(KeyboardButton("🛍 Mening xaridlarim"), KeyboardButton("ℹ️ Kafe haqida"))
            ),
            resizeKeyboard = true
        )
        bot.sendMessage(chatId, "Hurmatli mijoz kerakli bo'limni tanlang", replyMarkup = keyboard)
    }

    suspend fun getUserByNumber(number: String): UserData? {
        // Avval cache dan
        UserCacheManager.getUser(number)?.let {
            return it
        }

        // So'ng Firebase dan
        return suspendCoroutine { cont ->
            val ref = FirebaseDatabase.getInstance().getReference("mangal_clients")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val user = child.getValue(UserData::class.java)
                        if (user?.number == number) {
                            UserCacheManager.put(number, user)
                            cont.resume(user)
                            return
                        }
                    }
                    cont.resume(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resumeWithException(error.toException())
                }
            })
        }
    }

    //mangal_clients
    suspend fun getUserByChatId(chatId: String): UserData? = suspendCoroutine { cont ->
        val ref = FirebaseDatabase.getInstance().getReference("mangal_clients")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val user = child.getValue(UserData::class.java)
                    if (user?.chatId == chatId) {
                        cont.resume(user)
                        return
                    }
                }
                cont.resume(null)
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resumeWithException(error.toException())
            }
        })
    }

    private fun saveUserToDatabase(user: UserData) {
        val db = FirebaseDatabase.getInstance()
        val ref = db.getReference("mangal_clients")
        ref.child(user.uniqueId).setValueAsync(user)
    }

    fun getPhoneNumberFromCache(chatId: String): String? {
        val number = UserCacheManager.cache.entries
            .firstOrNull { it.value.chatId == chatId }
            ?.key

        return number
    }

    fun getUniqueIdFromCache(chatId: ChatId): String? {
        val chatIdStr = when (chatId) {
            is ChatId.Id -> chatId.id.toString()
            is ChatId.ChannelUsername -> chatId.username
        }

        return UserCacheManager.cache.values
            .firstOrNull { it.chatId == chatIdStr }
            ?.uniqueId
    }

    fun checkCafeStatus(chatId: ChatId) {
        val ref = FirebaseDatabase.getInstance().getReference("mangal_about").child("CRlCt5DVl0PWpgdVIoMLGasg0Yv2")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.child("open_closed").getValue(String::class.java)
                val message = when (status) {
                    "open" -> "✅ Kafe hozir ochiq."
                    "closed" -> "❌ Kafe hozir yopiq."
                    else -> "ℹ️ Kafe holati noma'lum."
                }

                bot.sendMessage(chatId = chatId, text = message)
            }

            override fun onCancelled(error: DatabaseError) {
                bot.sendMessage(chatId = chatId, text = "⚠️ Ma'lumotni olishda xatolik yuz berdi.")
            }
        })
    }

    fun cafeAbout(chatId: ChatId) {
        val loadingMsg = bot.sendMessage(chatId, "⏳ Kafe haqida ma'lumot olinmoqda...").getOrNull()
        val ref = FirebaseDatabase.getInstance()
            .getReference("mangal_about")
            .child("CRlCt5DVl0PWpgdVIoMLGasg0Yv2")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "-"
                val location = snapshot.child("location").getValue(String::class.java) ?: "-"
                val delivery = snapshot.child("delivery").getValue(String::class.java) ?: "-"
                val discount = snapshot.child("discount").getValue(String::class.java) ?: "-"
                val image = snapshot.child("image").getValue(String::class.java)
                val imageTwo = snapshot.child("imageTwo").getValue(String::class.java)
                val latitude = snapshot.child("latitude").getValue(String::class.java)
                val longitude = snapshot.child("longitude").getValue(String::class.java)
                val night = snapshot.child("night").getValue(String::class.java) ?: "-"
                val number = snapshot.child("number").getValue(String::class.java) ?: "-"
                val room = snapshot.child("room").getValue(String::class.java) ?: "-"

                val caption = """
                🍽 *$name*
                
                📍 $location
                🚚 Yetkazib berish: $delivery
                🎁 Chegirma: $discount
                🌙 Tungi xizmat: $night
                ☎️ Aloqa: $number
                🏢 Xonalar: $room
            """.trimIndent()

                val locationButton = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "📍 Lokatsiyani ko‘rish",
                            callbackData = "SHOW_LOCATION:$latitude:$longitude"
                        )
                    )
                )

                // Rasm 2 (alohida rasm sifatida)
                if (!image.isNullOrBlank()) {
                    bot.copyMessage(
                        fromChatId = ChatId.fromId(-1002561324885),
                        chatId = chatId,
                        messageId = image.toLong()
                    )
                }

                // Rasm 1
                if (!imageTwo.isNullOrBlank()) {
                    bot.copyMessage(
                        fromChatId = ChatId.fromId(-1002561324885),
                        chatId = chatId,
                        messageId = imageTwo.toLong(),
                        caption = caption,
                        parseMode = ParseMode.MARKDOWN,
                        replyMarkup = locationButton
                    )
                }
                loadingMsg?.let {
                    bot.deleteMessage(chatId, it.messageId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                bot.sendMessage(chatId = chatId, text = "⚠️ Ma'lumotni olishda xatolik yuz berdi.")
            }
        })
    }

    fun tradeHistory(bot: Bot, chatId: ChatId, uniqueId: String){
        val db = FirebaseDatabase.getInstance()
        val ref = db.getReference("mangal_clients").child(uniqueId)
        val refAbout = db.getReference("mangal_about").child("CRlCt5DVl0PWpgdVIoMLGasg0Yv2")

        refAbout.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val discount = snapshot.child("discount").getValue(String::class.java) ?: "-"

                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: "-"
                        val bought = snapshot.child("bought").getValue(String::class.java) ?: "-"

                        if (bought == ""){
                            bot.sendMessage(chatId = chatId, text = "Siz hali xarid amalga oshirmagansiz")
                            return
                        }
                        val count = snapshot.child("count").getValue(String::class.java) ?: "-"
                        val result = kotlin.math.abs(count.toInt() - discount.toInt())



                        val caption = """
                📝 $name
                
                💸 Hozirgi ballingiz: $count ball
                
                💰 Bonusgacha qoldi: $result ball
                
                📜 Jami ballingiz: $bought ball
            """.trimIndent()

                        bot.sendMessage(chatId = chatId, text = caption)
                    }

                    override fun onCancelled(p0: DatabaseError?) {
                        TODO("Not yet implemented")
                    }
                })
            }

            override fun onCancelled(p0: DatabaseError?) {
                TODO("Not yet implemented")
            }
        })
    }
}