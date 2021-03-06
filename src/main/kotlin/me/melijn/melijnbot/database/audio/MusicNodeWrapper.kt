package me.melijn.melijnbot.database.audio

import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class MusicNodeWrapper(val taskManager: TaskManager, private val musicNodeDao: MusicNodeDao) {

    val musicNodeCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NORMAL_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, String> { guildId ->
            getGainProfile(guildId)
        })

    private fun getGainProfile(guildId: Long): CompletableFuture<String> {
        val future = CompletableFuture<String>()

        taskManager.async {
            val profile = musicNodeDao.get(guildId)
            future.complete(profile)
        }

        return future
    }

    suspend fun setNode(guildId: Long, node: String) {
        musicNodeDao.insert(guildId, node)
        musicNodeCache.put(guildId, CompletableFuture.completedFuture(node))
    }

    suspend fun isPremium(guildId: Long): Boolean {
        return musicNodeCache.get(guildId).await() == "premium"
    }

}