package me.pectics.paper.plugin.permpacks.upload

import me.pectics.paper.plugin.permpacks.BinaryCache
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.util.SerializableURI
import me.pectics.paper.plugin.permpacks.util.sha1
import java.io.File

internal interface UploadService {

    /**
     * 文件上传服务的名称列表，可用于配置文件中指定服务
     */
    val names: List<String>

    /**
     * 启动文件上传服务
     *
     * @param context 提供服务所需配置的上下文
     * @throws Exception 启动过程中发生错误
     */
    fun launch(context: UploadServiceContext)

    /**
     * 停止文件上传服务，释放相关资源
     */
    fun shutdown()

    /**
     * 上传文件并返回其可访问的URI
     *
     * @param file 要上传的文件
     * @return 文件的可访问URI
     */
    fun upload(file: File): URI

    /**
     * 清理过期的文件，保留指定部分
     *
     * @param retain 要保留的文件项
     */
    fun cleanup(retain: Iterable<FilePackItem>)

    companion object {

        private lateinit var service: UploadService

        /**
         * 检查上传服务是否已初始化
         */
        fun available() = ::service.isInitialized

        /**
         * 初始化上传服务
         *
         * @param service 要使用的上传服务实例
         * @param context 提供服务所需配置的上下文
         */
        fun initialize(service: UploadService, context: UploadServiceContext) {
            service.launch(context)
            this.service = service
        }

        /**
         * 停止上传服务
         */
        fun shutdown() = if (available()) service.shutdown() else Unit

        /**
         * 获取缓存的文件项的可访问URL
         *
         * @param item 文件项
         * @return 文件的可访问URI，若未缓存则为null
         */
        fun urlOf(item: FilePackItem): SerializableURI? = BinaryCache[item.hash.value]

        /**
         * 清理过期的文件，保留指定部分
         *
         * @param retain 要保留的文件项
         */
        fun cleanup(retain: Iterable<FilePackItem>) = if (available()) service.cleanup(retain) else Unit
    }
}