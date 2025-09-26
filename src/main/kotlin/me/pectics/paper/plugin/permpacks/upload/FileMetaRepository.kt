package me.pectics.paper.plugin.permpacks.upload

import me.pectics.paper.plugin.permpacks.PermPacks
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.util.sha1
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal object FileMetaRepository {

    private lateinit var repo: File
    private val metas = ConcurrentHashMap<Sha1Hex, FileMeta>()

    fun initialize(plugin: PermPacks) {
        repo = File(plugin.dataFolder, ".filerepo")
        require(repo.isDirectory || repo.mkdirs()) {
            "Could not create file repository directory: ${repo.absolutePath}"
        }
        bootstrapFromDisk()
    }

    private fun bootstrapFromDisk() {
        repo.listFiles()
            ?.asSequence()
            ?.filter(File::isFile)
            ?.forEach { file ->
                val name = file.name
                val isSha1 = name.length == 40 && name.all { it.isDigit() || it in 'a'..'f' }
                if (isSha1) {
                    val hash = Sha1Hex.of(name).getOrElse { return@forEach }
                    metas[hash] = FileMeta(name, file, file.length())
                }
            }
    }

    operator fun get(hash: Sha1Hex) = metas[hash]

    operator fun set(hash: Sha1Hex, meta: FileMeta) {
        metas[hash] = meta
    }

    operator fun contains(hash: Sha1Hex) = hash in metas.keys

    fun push(file: File) {
        val hash = file.sha1()
        val repoFile = repo.resolve(hash.value)
        synchronized(this) {
            if (!repoFile.exists()) {
                file.copyTo(repoFile, overwrite = true)
            }
            metas[hash] = FileMeta(file.name, repoFile, file.length())
        }
    }

    fun cleanup(retain: Set<Sha1Hex>) {
        val retainStrings = retain.map(Sha1Hex::value).toSet()

        // delete files on disk not retained
        repo.listFiles()
            ?.filter { it.isFile }
            ?.forEach { file ->
                val name = file.name
                val looksLikeSha1 = name.length == 40 && name.all { it.isDigit() || it in 'a'..'f' }
                if (looksLikeSha1 && name !in retainStrings) {
                    file.delete()
                }
            }

        // rebuild metas map to kept ones only
        metas.keys
            .filter { it.value !in retainStrings }
            .toList()
            .forEach { metas.remove(it) }
    }
}
