package me.melijn.melijnbot.database.supporter

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.threading.TaskManager

class SupporterWrapper(val taskManager: TaskManager, private val userSupporterDao: UserSupporterDao) {

    var supporters: Set<Supporter> = setOf()
    var userSupporterIds: Set<Long> = setOf()
    var guildSupporterIds: Set<Long> = setOf()

    init {
        DaoManager.afterTableFunctions.add {
            userSupporterDao.getSupporters {
                supporters = it
                userSupporterIds = it.map { supporter -> supporter.userId }.toSet()
                guildSupporterIds = it.map { supporter -> supporter.guildId }.toSet()
            }
        }
    }

    suspend fun add(userId: Long) {
        if (!userSupporterIds.contains(userId)) {
            userSupporterIds = userSupporterIds + userId
            userSupporterDao.addUser(userId)
        }
    }

    suspend fun remove(userId: Long) {
        if (userSupporterIds.contains(userId)) {
            userSupporterIds = userSupporterIds - userId
            userSupporterDao.removeUser(userId)
        }
    }

    suspend fun setGuild(authorId: Long, guildId: Long) {
        guildSupporterIds = guildSupporterIds + guildId
        val lastGuildPickTime = System.currentTimeMillis()
        val supportersInstance = supporters.toMutableList()

        val aSupporter = supportersInstance.first { it.userId == authorId }
        supportersInstance.remove(aSupporter)
        supportersInstance.add(Supporter(
            authorId,
            guildId,
            aSupporter.startMillis,
            lastGuildPickTime
        ))
        supporters = supportersInstance.toSet()

        userSupporterDao.setGuild(authorId, guildId, lastGuildPickTime)
    }
}
