package me.pectics.paper.plugin.permpacks

import com.comphenix.protocol.ProtocolLibrary
import me.pectics.paper.plugin.permpacks.command.Commands
import me.pectics.paper.plugin.permpacks.packet.PackBlocker
import me.pectics.paper.plugin.permpacks.packet.PackPacketTracker
import me.pectics.paper.plugin.permpacks.upload.FileMetaRepository
import me.pectics.paper.plugin.permpacks.upload.UploadService
import me.pectics.paper.plugin.permpacks.upload.selfhost.SelfHostService
import org.bukkit.plugin.java.JavaPlugin

class PermPacks: JavaPlugin() {

    override fun onEnable() {
        // Initialize components
        BinaryCache.initialize(this)
        FileMetaRepository.initialize(this)
        Options.initialize(this)

        // Initialize upload service if enabled
        if (Options.fileUploadEnabled) {
            Options.fileUploadService
                ?.let(String::lowercase)
                ?.apply {
                    val context = Options.fileUploadContext ?: emptyMap()
                    when (this) {
                        "self_host" -> UploadService.initialize(SelfHostService, context)
                        // TODO amazon_s3
                        else -> logger.warning("Unknown file upload service: $this")
                    }
                }
                ?: logger.warning("File upload service is not specified.")
        }

        // Load packs
        Options.loadPacks()

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
