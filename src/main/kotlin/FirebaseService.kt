package uz.ibrohim.food

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import uz.ibrohim.food.data.UserData
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseService {
    init {
        val serviceAccount = FileInputStream("firebase_adminsdk.json")
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl("https://jizzax-lor-default-rtdb.firebaseio.com/") // <- Realtime DB URL
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }
}


//fun handleForwardedMessage(dispatcher: Dispatcher) {
//    dispatcher.message {
//        val forwardedChat = message.forwardFromChat
//        if (forwardedChat != null) {
//            val channelId = forwardedChat.id
//            bot.sendMessage(
//                chatId = ChatId.fromId(message.chat.id),
//                text = "🆔 Kanal Chat ID: `$channelId`",
//                parseMode = ParseMode.MARKDOWN
//            )
//            println("🔍 Kanal chat ID: $channelId")
//        } else {
//            bot.sendMessage(
//                chatId = ChatId.fromId(message.chat.id),
//                text = "❌ Iltimos, kanal xabarini botga forward qiling."
//            )
//        }
//    }
//}