package com.excelsior.pocketvault.core.common

import android.net.Uri
import java.net.URI
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object VaultFormatters {
    private val dateFormat = SimpleDateFormat("yyyy.MM.dd · HH:mm", Locale.CHINA)
    private val compactDateTimeFormat = SimpleDateFormat("yyyy/MM/dd/HH:mm", Locale.CHINA)

    fun formatDateTime(timestamp: Long): String = dateFormat.format(Date(timestamp))
    fun formatDateTimeCompact(timestamp: Long): String = compactDateTimeFormat.format(Date(timestamp))

    fun hostFromUrl(url: String): String = runCatching {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        URI(normalized).host?.removePrefix("www.") ?: url
    }.getOrDefault(url)

    fun compactUrl(url: String): String = url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")

    fun passwordStrength(password: String): Int {
        var score = 0
        if (password.length >= 8) score++
        if (password.any(Char::isUpperCase)) score++
        if (password.any(Char::isDigit)) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return score.coerceIn(0, 4)
    }

    fun passwordStrengthLabel(score: Int): String = when (score) {
        0, 1 -> "偏弱"
        2 -> "中等"
        3 -> "较强"
        else -> "很强"
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun initials(title: String): String = title
        .trim()
        .take(2)
        .uppercase(Locale.getDefault())

    fun aspectRatioText(value: Float): String = "${(value * 100).roundToInt() / 100f}"

    fun isValidUrl(value: String): Boolean {
        val normalized = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"
        return runCatching { URI(normalized).host?.contains('.') == true }.getOrDefault(false)
    }

    fun safeUri(value: String?): Uri? = value?.takeIf { it.isNotBlank() }?.let(Uri::parse)

    val welcomeLines = listOf(
        "先记下来，整理可以晚一点。",
        "像便签一样轻，像收藏夹一样稳。",
        "灵感、链接与秘密，都值得被安静收好。",
        "把零碎写成秩序，也把细节留得好看。",
    )
}
