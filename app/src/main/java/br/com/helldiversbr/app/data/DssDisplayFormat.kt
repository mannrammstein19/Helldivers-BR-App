package br.com.helldiversbr.app.data

import java.util.Locale

/** Somente apresentação de uma duração já confirmada/estimada; não decide estados. */
object DssDisplayFormat {
    fun duration(seconds: Long): String {
        if (seconds < 0) return ""
        val days = seconds / 86_400L
        val hours = seconds % 86_400L / 3_600L
        val minutes = seconds % 3_600L / 60L
        val remainder = seconds % 60L
        return if (days > 0) "%dd %02dh %02dmin %02ds".format(Locale.ROOT, days, hours, minutes, remainder)
        else "%02dh %02dmin %02ds".format(Locale.ROOT, hours, minutes, remainder)
    }
}
