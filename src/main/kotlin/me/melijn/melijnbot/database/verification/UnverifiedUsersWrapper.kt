package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.objects.threading.TaskManager

class UnverifiedUsersWrapper(val taskManager: TaskManager, private val unverifiedUsersDao: UnverifiedUsersDao) {

    suspend fun getMoment(guildId: Long, userId: Long): Long {
        return unverifiedUsersDao.getMoment(guildId, userId)
    }

    suspend fun getTries(guildId: Long, userId: Long): Long {
        return unverifiedUsersDao.getTries(guildId, userId)
    }

    suspend fun remove(guildId: Long, userId: Long) {
        unverifiedUsersDao.remove(guildId, userId)
    }

    suspend fun add(guildId: Long, userId: Long) {
        unverifiedUsersDao.add(guildId, userId)
    }

    suspend fun contains(guildId: Long, userId: Long): Boolean {
        return unverifiedUsersDao.contains(guildId, userId)
    }

    suspend fun update(guildId: Long, userId: Long, tries: Long) {
        unverifiedUsersDao.update(guildId, userId, tries)
    }
}