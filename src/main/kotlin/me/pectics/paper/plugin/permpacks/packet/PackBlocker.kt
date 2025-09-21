package me.pectics.paper.plugin.permpacks.packet

import com.comphenix.protocol.PacketType.Play.Server.ADD_RESOURCE_PACK
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import me.pectics.paper.plugin.permpacks.PermPacks
import me.pectics.paper.plugin.permpacks.util.logger

internal class PackBlocker(plugin: PermPacks) : PacketAdapter(plugin, ListenerPriority.HIGHEST, ADD_RESOURCE_PACK) {

    private val log = logger<PackBlocker>()

    override fun onPacketSending(event: PacketEvent?) {
        val packet = event?.packet ?: return
        when (packet.type) {
            ADD_RESOURCE_PACK -> {
                val player = event.player
                val url = packet.strings.readSafely(0) ?: return
                val hash = packet.strings.readSafely(1)
                if (!PackPacketTracker.check(player, url, hash)) {
                    event.isCancelled = true
                    log.info("Blocked a resource pack send to ${player.name} ($url, $hash)")
                }
            }
        }
    }
}
