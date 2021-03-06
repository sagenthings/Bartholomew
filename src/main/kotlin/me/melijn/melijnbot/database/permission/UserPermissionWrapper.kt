package me.melijn.melijnbot.database.permission

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.IMPORTANT_CACHE
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class UserPermissionWrapper(val taskManager: TaskManager, private val userPermissionDao: UserPermissionDao) {

    val guildUserPermissionCache = CacheBuilder.newBuilder()
        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Pair<Long, Long>, Map<String, PermState>> { key ->
            getPermissionList(key)
        })

    private fun getPermissionList(guildAndUser: Pair<Long, Long>): CompletableFuture<Map<String, PermState>> {
        val languageFuture = CompletableFuture<Map<String, PermState>>()
        taskManager.async {
            val map = userPermissionDao.getMap(guildAndUser.first, guildAndUser.second)
            languageFuture.complete(map)
        }
        return languageFuture
    }

    fun setPermissions(guildId: Long, userId: Long, permissions: List<String>, state: PermState) {
        val pair = Pair(guildId, userId)
        val permissionMap = guildUserPermissionCache.get(pair).get().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissions.forEach { permissionMap.remove(it) }
            userPermissionDao.bulkDelete(guildId, userId, permissions)
        } else {
            permissions.forEach { permissionMap[it] = state }
            userPermissionDao.bulkPut(guildId, userId, permissions, state)
        }
        guildUserPermissionCache.put(pair, CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun setPermission(guildId: Long, userId: Long, permission: String, state: PermState) {
        val pair = Pair(guildId, userId)
        val permissionMap = guildUserPermissionCache.get(pair).get().toMutableMap()
        if (state == PermState.DEFAULT) {
            permissionMap.remove(permission)
            userPermissionDao.delete(guildId, userId, permission)
        } else {
            permissionMap[permission] = state
            userPermissionDao.set(guildId, userId, permission, state)
        }
        guildUserPermissionCache.put(pair, CompletableFuture.completedFuture(permissionMap.toMap()))
    }

    suspend fun clear(guildId: Long, userId: Long) {
        guildUserPermissionCache.put(Pair(guildId, userId), CompletableFuture.completedFuture(emptyMap()))
        userPermissionDao.delete(guildId, userId)
    }

    fun setPermissions(guildId: Long, userId: Long, permissions: Map<String, PermState>) {
        for (state in PermState.values()) {
            setPermissions(guildId, userId, permissions.filter { entry ->
                entry.value == state
            }.keys.toList(), state)
        }
    }
}