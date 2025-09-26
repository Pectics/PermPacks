package me.pectics.paper.plugin.permpacks

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.pectics.paper.plugin.permpacks.util.logger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 持久化存储的缓存文件管理器
 */
internal class BinaryCache private constructor(plugin: PermPacks) {

    private val log = logger("BinaryCacher")

    private val file = plugin.dataFolder.resolve(".cache")
    private val map = ConcurrentHashMap<String, String>()

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "PermPacks-BinaryCache").apply { isDaemon = true }
        }
    private val pendingWrite = AtomicReference<ScheduledFuture<*>?>(null)
    private val debounceMillis = 500L

    init {
        require(file.isFile || file.createNewFile()) {
            "Could not create cache file: ${file.absolutePath}"
        }
        ensureSafety()
        loadFromDisk()
    }

    private inline fun input(block: DataInputStream.() -> Unit) =
        file.inputStream().use { DataInputStream(it).use(block) }

    private inline fun output(block: DataOutputStream.() -> Unit) =
        file.outputStream().use { DataOutputStream(it).use(block) }

    private fun ensureSafety() {
        if (!file.isFile)
            file.createNewFile()
        if (file.length() == 0L)
            output { writeInt(0) }
    }

    private fun loadFromDisk() {
        try {
            input {
                val size = readInt()
                repeat(size) {
                    val k = readUTF()
                    val v = readUTF()
                    map[k] = v
                }
            }
        } catch (_: EOFException) {
            map.clear()
            output { writeInt(0) }
        } catch (e: Throwable) {
            log.warning("Failed to load cache from disk", e)
        }
    }

    private fun scheduleWrite() {
        pendingWrite.getAndSet(null)?.cancel(false)
        val future = scheduler.schedule(::persistNow, debounceMillis, TimeUnit.MILLISECONDS)
        pendingWrite.set(future)
    }

    private fun persistNow() {
        val entries = map.entries.map { it.key to it.value }
        try {
            output {
                writeInt(entries.size)
                for ((k, v) in entries) {
                    writeUTF(k)
                    writeUTF(v)
                }
            }
        } catch (e: Throwable) {
            log.error("Failed to persist cache to disk", e)
        }
    }

    /**
     * 将缓存内容立即写入磁盘
     */
    fun flush() {
        pendingWrite.getAndSet(null)?.cancel(false)
        persistNow()
    }

    /**
     * 将缓存内容写入磁盘并关闭调度器
     */
    fun shutdown() {
        try {
            flush()
        } finally {
            scheduler.shutdown()
        }
    }

    companion object {

        private lateinit var instance: BinaryCache

        /**
         * 初始化缓存管理器
         */
        fun initialize(plugin: PermPacks) {
            instance = BinaryCache(plugin)
        }

        /**
         * 安全关闭缓存管理器
         */
        fun shutdown() {
            if (::instance.isInitialized)
                instance.shutdown()
        }

        /**
         * 获取缓存值
         *
         * @return 反序列化后的对象，若键不存在或反序列化失败则返回 null
         */
        inline operator fun <reified T> get(key: String): T? =
            instance.map[key]
                ?.runCatching { Json.decodeFromString<T>(this) }
                ?.getOrNull()

        /**
         * 设置缓存值，设置为 null 则移除键
         *
         * 防抖写入，无IO阻塞
         */
        inline operator fun <reified T> set(key: String, value: T?) {
            // 若值为 null 则尝试移除键，若原本就不存在则不写入
            if (value == null) {
                if (instance.map.remove(key) == null) return
                instance.scheduleWrite()
            }
            // 非 null 则写入
            instance.map[key] = Json.encodeToString(value)
            instance.scheduleWrite()
        }
    }
}
