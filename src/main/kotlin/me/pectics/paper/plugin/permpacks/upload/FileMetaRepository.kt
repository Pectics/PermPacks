package me.pectics.paper.plugin.permpacks.upload

import me.pectics.paper.plugin.permpacks.PermPacks
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.util.sha1
import java.io.File

internal class FileMetaRepository(plugin: PermPacks) {

    private val repo = File(plugin.dataFolder, ".filerepo")

    private val metas = mutableMapOf<Sha1Hex, FileMeta>()

    init {
        require(repo.isDirectory || repo.mkdirs()) {
            "Could not create file repository directory: ${repo.absolutePath}"
        }
    }

    companion object {

        private lateinit var instance: FileMetaRepository

        fun initialize(plugin: PermPacks) {
            instance = FileMetaRepository(plugin)
        }

        operator fun get(hash: Sha1Hex) = instance.metas[hash]
        operator fun set(hash: Sha1Hex, meta: FileMeta) = instance.metas.set(hash, meta)
        operator fun contains(hash: Sha1Hex) = hash in instance.metas

        fun push(file: File) {
            instance.apply {
                val hash = file.sha1()
                val repoFile = repo.resolve(hash.value)
                if (!repoFile.exists())
                    file.copyTo(repoFile, overwrite = true)
                metas[hash] = FileMeta(file.name, repoFile, file.length())
            }
        }

        fun clear() {
            instance.metas.clear()
            instance.repo.listFiles()?.forEach(File::deleteRecursively)
        }

    }

}