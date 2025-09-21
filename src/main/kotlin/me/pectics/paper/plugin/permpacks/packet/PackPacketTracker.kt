package me.pectics.paper.plugin.permpacks.packet

import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.PackItem
import me.pectics.paper.plugin.permpacks.data.UrlPackItem
import me.pectics.paper.plugin.permpacks.upload.UploadService
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal object PackPacketTracker {

    private const val CLEANUP_THRESHOLD_MS = 15_000L // 15s

    // key -> count
    private val counts = ConcurrentHashMap<String, AtomicInteger>()
    // key -> last mark time (ms)
    private val lastSeen = ConcurrentHashMap<String, Long>()

    private fun key(playerId: UUID, url: String?, hash: String?) =
        "${playerId}|${url ?: "<null>"}|${hash ?: "<null>"}"

    private fun mark(player: Player, url: String?, hash: String?) {
        val key = key(player.uniqueId, url, hash)
        counts.compute(key) { _, value ->
            (value ?: AtomicInteger(0))
                .apply { incrementAndGet() }
        }
        lastSeen[key] = System.currentTimeMillis()
    }

    fun mark(player: Player, item: PackItem) {
        when (item) {
            is UrlPackItem -> mark(player, item.url.toString(), item.hash?.value)
            is FilePackItem -> mark(player, UploadService.urlOf(item).toString(), item.hash.value)
        }
    }

    /**
     * 检查是否被标记，若被标记则清除
     */
    fun check(player: Player, url: String?, hash: String?): Boolean {
        val key = key(player.uniqueId, url, hash)
        val counter = counts[key] ?: return false
        val left = counter.decrementAndGet()
        if (left <= 0) {
            counts.remove(key)
            lastSeen.remove(key)
        } else {
            lastSeen[key] = System.currentTimeMillis()
        }
        return true
    }

    /** 需要在插件启用时调用一次，定期清理超时条目 */
    fun startCleanupTask(plugin: JavaPlugin) {
        plugin.server.scheduler.runTaskTimerAsynchronously(plugin, Runnable {
            val now = System.currentTimeMillis()
            val iterator = lastSeen.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > CLEANUP_THRESHOLD_MS) {
                    counts.remove(entry.key)
                    iterator.remove()
                }
            }
        }, 20L * 5, 20L * 5)
    }
}
