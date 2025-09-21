package me.pectics.paper.plugin.permpacks.pack

import me.pectics.paper.plugin.permpacks.data.Pack
import me.pectics.paper.plugin.permpacks.packet.PackPacketTracker
import me.pectics.paper.plugin.permpacks.util.toRequest
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

object Packer {

    // Cached packs sorted by priority
    private val packs: MutableList<Pack> = mutableListOf()
    // Distributed packs
    private val distributed: MutableMap<UUID, List<Pack>> = mutableMapOf()

    fun cache(packs: Set<Pack>) {
        this.packs.clear()
        this.packs.addAll(packs)
        this.packs.sortBy(Pack::priority)
    }

    fun getDistributed(player: Player) = distributed[player.uniqueId]

    fun distribute(player: Player) {
        val request = packs
            .filter { player.hasPermission(it.permission) }
            .ifEmpty { return }
            .also { distributed[player.uniqueId] = it }
            .apply { this
                .flatMap { it.items }
                .forEach { PackPacketTracker.mark(player, it) }
            }
            .toRequest()
        player.sendResourcePacks(request)
    }

    fun distribute() = Bukkit.getOnlinePlayers().forEach(::distribute)

}