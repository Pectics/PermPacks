package me.pectics.paper.plugin.permpacks.util

import me.pectics.paper.plugin.permpacks.PermPacks
import java.util.logging.Level
import java.util.logging.Logger

internal inline fun <reified T> logger() = PermPacksLogger.of(T::class.let { it.simpleName ?: it.java.simpleName })
internal fun logger(who: String) = PermPacksLogger.of(who)

internal class PermPacksLogger private constructor(who: String?) : Logger(PermPacks::class.simpleName, null) {

    init { useParentHandlers = true }

    private val prefix = if (who != null) "[$who] " else ""

    override fun info(msg: String?) {
        super.info(prefix + msg)
    }

    override fun warning(msg: String?) {
        super.warning(prefix + msg)
    }

    override fun severe(msg: String?) {
        super.severe(prefix + msg)
    }

    override fun fine(msg: String?) {
        super.fine(prefix + msg)
    }

    override fun finer(msg: String?) {
        super.finer(prefix + msg)
    }

    override fun finest(msg: String?) {
        super.finest(prefix + msg)
    }

    override fun config(msg: String?) {
        super.config(prefix + msg)
    }

    override fun log(level: Level?, msg: String?) {
        super.log(level, prefix + msg)
    }

    override fun log(level: Level?, msg: String?, thrown: Throwable?) {
        super.log(level, prefix + msg, thrown)
    }

    companion object {
        fun of(who: String): PermPacksLogger {
            return PermPacksLogger(who)
        }
    }

}