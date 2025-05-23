package uz.ibrohim.food.data

data class UserData(
    var name: String = "",
    var number: String = "",
    var chatId: String = "",
    var uniqueId: String = "",
    val messageId: String = "",
    val fromChatId: String = "",
    var bought: String = ""
)