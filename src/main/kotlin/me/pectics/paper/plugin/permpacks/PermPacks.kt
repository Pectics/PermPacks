package me.pectics.paper.plugin.permpacks

import com.comphenix.protocol.ProtocolLibrary
import me.pectics.paper.plugin.permpacks.command.Commands
import me.pectics.paper.plugin.permpacks.packet.PackBlocker
import me.pectics.paper.plugin.permpacks.packet.PackPacketTracker
import me.pectics.paper.plugin.permpacks.upload.FileMetaRepository
import me.pectics.paper.plugin.permpacks.upload.UploadService
import me.pectics.paper.plugin.permpacks.upload.UploadServiceContext
import me.pectics.paper.plugin.permpacks.upload.selfhost.SelfHostService
import me.pectics.paper.plugin.permpacks.upload.s3.S3Service
import me.pectics.paper.plugin.permpacks.util.warning
import org.bukkit.plugin.java.JavaPlugin

class PermPacks : JavaPlugin() {

    fun uploadServiceInit() {
        if (!Options.fileUploadEnabled) return
        val service = Options.fileUploadService?.lowercase() ?: run {
            logger.warning("File upload service is not specified.")
            return
        }
        // Catching service exceptions
        val context = Options.fileUploadContext
            ?.let(UploadServiceContext::of)
            ?: UploadServiceContext.EMPTY
        runCatching {
            when (service) {
                in SelfHostService.names -> UploadService.initialize(SelfHostService, context)
                in S3Service.names -> UploadService.initialize(S3Service, context)
                else -> logger.warning("Unknown file upload service: $service")
            }
        }.onFailure {
            logger.warning("Failed to initialize file upload service.", it)
        }
    }

    override fun onEnable() {
        // Initialize components
        BinaryCache.initialize(this)
        FileMetaRepository.initialize(this)
        Options.initialize(this)

        // Initialize upload service
        uploadServiceInit()

        // Load packs
        Options.loadPacks()

        // Cleanup remote uploads if enabled
        if (Options.fileUploadEnabled && Options.uploadCleanupEnabled) {
            val retain = Options.retainHashes()
            UploadService.cleanup(retain)
        }

        // Hook ProtocolLib if available
        server.pluginManager.getPlugin("ProtocolLib")
            ?.let {
                if (Options.blockOtherPacks) {
                    val manager = ProtocolLibrary.getProtocolManager()
                    manager.addPacketListener(PackBlocker(this))
                    PackPacketTracker.startCleanupTask(this)
                }
                logger.info("ProtocolLib hooked.")
            }
            ?: logger.warning("ProtocolLib not found, some features may not work properly.")

        Commands.register()
    }

    override fun onDisable() {
        Commands.unregister()
        server.asyncScheduler.cancelTasks(this)
        server.scheduler.cancelTasks(this)
        UploadService.shutdown()
        BinaryCache.shutdown()
    }

}
