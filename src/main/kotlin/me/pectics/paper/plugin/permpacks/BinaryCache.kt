package me.pectics.paper.plugin.permpacks

import kotlinx.serialization.json.Json
import me.pectics.paper.plugin.permpacks.util.logger
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level

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
            log.log(Level.WARNING, "Failed to load cache from disk", e)
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
            e.printStackTrace()
        }
    }

    fun flush() {
        pendingWrite.getAndSet(null)?.cancel(false)
        persistNow()
    }

    fun shutdown() {
        try {
            flush()
        } finally {
            scheduler.shutdown()
        }
    }

    companion object {

        private lateinit var instance: BinaryCache

        fun initialize(plugin: PermPacks) {
            instance = BinaryCache(plugin)
        }

        fun shutdown() {
            if (::instance.isInitialized)
                instance.shutdown()
        }

        inline operator fun <reified T> get(key: String): T? =
            instance.map[key]?.let(Json::decodeFromString)

        operator fun set(key: String, value: Any) {
            instance.map[key] = Json.encodeToString(value)
            instance.scheduleWrite()
        }

        fun remove(key: String) {
            if (instance.map.remove(key) != null)
                instance.scheduleWrite()
        }

    }
}
