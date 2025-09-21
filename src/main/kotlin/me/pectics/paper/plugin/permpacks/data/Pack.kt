package me.pectics.paper.plugin.permpacks.data

data class Pack(
    val id: String,
    val permission: String,
    val items: List<PackItem>,
    val priority: Int,
)
