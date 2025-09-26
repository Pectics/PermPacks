package me.pectics.paper.plugin.permpacks

import com.comphenix.protocol.ProtocolLibrary
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
import me.pectics.paper.plugin.permpacks.command.Commands
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.Pack
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

    override fun onLoad() {
        CommandAPI.onLoad(CommandAPIBukkitConfig(this))
    }

    override fun onEnable() {
        // 初始化 CommandAPI
        CommandAPI.onEnable()

        // 插件数据文件夹
        if (!dataFolder.isDirectory && !dataFolder.mkdirs())
            throw IllegalStateException("Could not create plugin data folder: ${dataFolder.absolutePath}")

        // 初始化基础组件
        BinaryCache.initialize(this)
        FileMetaRepository.initialize(this)
        Options.initialize(this)

        initUploadService()

        // 读取资源包配置
        Options.readPacks()

        // 清理过期的上传文件
        if (Options.fileUploadEnabled && Options.fileUploadCleanup) {
            val retain = Options.packs
                .flatMap(Pack::items)
                .filterIsInstance<FilePackItem>()
            UploadService.cleanup(retain)
        }

        // 集成 ProtocolLib 以拦截资源包相关数据包
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

        // 注册命令
        Commands.register()
    }

    fun initUploadService() {
        // 检查文件上传配置
        if (!Options.fileUploadEnabled) return
        val service = Options.fileUploadService?.lowercase() ?: run {
            logger.warning("File upload service is not specified.")
            return
        }
        // 获取配置上下文
        val context = Options.fileUploadContext
            ?.let(UploadServiceContext::of)
            ?: UploadServiceContext.EMPTY
        // 初始化上传服务，捕获异常
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

    override fun onDisable() {
        Commands.unregister()
        server.asyncScheduler.cancelTasks(this)
        server.scheduler.cancelTasks(this)
        UploadService.shutdown()
        BinaryCache.shutdown()
    }

}
