package me.pectics.paper.plugin.permpacks.command

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.arguments.AbstractArgument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.commandAPICommand
import dev.jorel.commandapi.kotlindsl.multiLiteralArgument
import dev.jorel.commandapi.kotlindsl.playerArgument
import dev.jorel.commandapi.kotlindsl.stringArgument
import dev.jorel.commandapi.kotlindsl.subcommand
import me.pectics.paper.plugin.permpacks.Options
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.Pack
import me.pectics.paper.plugin.permpacks.data.UrlPackItem
import me.pectics.paper.plugin.permpacks.pack.Packer
import org.bukkit.entity.Player

private inline fun <reified T, reified I: AbstractArgument<T,I,A,S>, reified A: AbstractArgument<*,*,A,S>, reified S>
        AbstractArgument<T,I,A,S>.suggestions(suggestions: Collection<String>) =
    replaceSuggestions(ArgumentSuggestions.strings(suggestions))

internal object Commands {

    fun register() {
        commandAPICommand("permpacks") {
            withAliases("ppacks", "pp")
            withPermission("permpacks.admin")

            subcommand("reload") {
                multiLiteralArgument("RELOAD_OPTION", "configs", "packs")
                anyExecutor { sender, args ->
                    val option = args["RELOAD_OPTION"] as String
                    when (option) {
                        "configs" -> {
                            Options.reload()
                            sender.sendMessage("Reloaded configs.")
                        }
                        "packs" -> {
                            Packer.cache(Options.packs)
                            Packer.distribute()
                            sender.sendMessage("Redistributed packs.")
                        }
                    }
                }
            }

            subcommand("info") {
                stringArgument("PACK_ID") {
                    suggestions(Options.packs.map(Pack::id))
                }
                anyExecutor { sender, args ->
                    val packId = args["PACK_ID"] as String
                    val pack = Options.packs.find { it.id == packId }
                    if (pack == null) {
                        sender.sendMessage("Pack \"$packId\" not found.")
                        return@anyExecutor
                    }
                    sender.sendMessage("Pack Info(${pack.id}):")
                    sender.sendMessage(" - Permission: ${pack.permission}")
                    sender.sendMessage(" - Items(${pack.items.size}):")
                    pack.items.forEach {
                        when (it) {
                            is UrlPackItem -> {
                                sender.sendMessage("   - URL: ${it.url}")
                                sender.sendMessage("     Hash: ${it.hash ?: "<undefined>"}")
                            }
                            is FilePackItem -> {
                                sender.sendMessage("   - File: ${it.file.name}")
                                sender.sendMessage("     Hash: ${it.hash.value}")
                            }
                            // TODO other pack item types
                        }
                    }
                }
            }

            subcommand("check") {
                playerArgument("PLAYER")
                anyExecutor { sender, args ->
                    val player = args["PLAYER"] as Player
                    val distributed = Packer.getDistributed(player) ?: emptyList()
                    sender.sendMessage("Player Pack Stack(${player.name}):")
                    Options.packs
                        .filter { player.hasPermission(it.permission) }
                        .sortedBy { it.priority }
                        .forEach {
                            val status = if (it in distributed) "✔" else "✘"
                            sender.sendMessage(" - ${it.id}(${it.permission}) [$status]")
                        }
                }
            }

        }
    }

    fun unregister() {
        CommandAPI.unregister("permpacks")
    }

}