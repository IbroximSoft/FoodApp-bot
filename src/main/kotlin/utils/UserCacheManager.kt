package uz.ibrohim.food.utils

import uz.ibrohim.food.data.UserData

object UserCacheManager {
    val cache = mutableMapOf<String, UserData>()

    fun getUser(phone: String): UserData? {
        return cache[phone]
    }

    fun saveUser(key: String, user: UserData) {
        cache[key] = user
    }

    fun put(key: String, user: UserData) {
        cache[key] = user
    }
}