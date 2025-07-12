/**
 * Created by ST on 2/12/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.views.extensions

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.Base64
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.text.HtmlCompat
import com.google.gson.internal.`$Gson$Preconditions`
import org.jsoup.Jsoup
import java.io.InputStream


object StringExtension {
    fun String.fromURI(): Spanned = HtmlCompat.fromHtml(
        this,
        HtmlCompat.FROM_HTML_MODE_LEGACY
    )

    fun String.extractHostname(): String? {
        val regex = """Unable to resolve host "([^"]+)"""".toRegex()
        return regex.find(this)?.groups?.get(1)?.value
    }

    fun List<String>.findSidInHeaders(): String =
        this.find { it.startsWith("SID=") }?.substringBefore(";") ?: ""

    /** Extract latitude and longitude from a URL */


    fun String.convertHtmlToSpannableStringBuilder(): SpannableStringBuilder? {
        // Replace '\n' with '<br>' to preserve line breaks
        val processedString = this.replace("\n", "<br>")
        // Convert HTML to CharSequence and then to SpannableStringBuilder
        val sequence: CharSequence = Html.fromHtml(processedString, Html.FROM_HTML_MODE_LEGACY)
        return SpannableStringBuilder(sequence).trimSpannable()
    }

    private fun SpannableStringBuilder.trimSpannable(): SpannableStringBuilder? {
        `$Gson$Preconditions`.checkNotNull(this)
        var trimStart = 0
        var trimEnd = 0
        var text = this.toString()
        while (text.isNotEmpty() && text.startsWith("\n")) {
            text = text.substring(1)
            trimStart += 1
        }
        while (text.isNotEmpty() && text.endsWith("\n")) {
            text = text.substring(0, text.length - 1)
            trimEnd += 1
        }
        return this.delete(0, trimStart).delete(this.length - trimEnd, this.length)
    }

    fun Pair<Double, Double>?.toBundle(): Bundle {
        return Bundle().apply {
            this@toBundle?.let { latLong ->
                putDouble("latitude", latLong.first)
                putDouble("longitude", latLong.second)
            }
        }
    }

    fun Bundle.toLatLong(): Pair<Double, Double>? {
        return if (containsKey("latitude") && containsKey("longitude")) {
            Pair(getDouble("latitude"), getDouble("longitude"))
        } else null
    }

    fun Pair<Double, Double>?.toNavString(): String {
        return this?.let { "${it.first},${it.second}" } ?: ""
    }


    fun String.toLatLong(): Pair<Double, Double>? {
        val parts = this.split(",")
        return if (parts.size == 2) {
            parts[0].toDoubleOrNull()?.let { lat ->
                parts[1].toDoubleOrNull()?.let { lon ->
                    Pair(lat, lon)
                }
            }
        } else null
    }

    fun String.extractLatLon1(): Pair<Double, Double>? {
        val latPattern = "mlat=([\\d.]+)".toRegex()
        val lonPattern = "mlon=([\\d.]+)".toRegex()

        val latMatchResult = latPattern.find(this)
        val lonMatchResult = lonPattern.find(this)

        val lat = latMatchResult?.groups?.get(1)?.value?.toDoubleOrNull()
        val lon = lonMatchResult?.groups?.get(1)?.value?.toDoubleOrNull()

        return if (lat != null && lon != null) {
            Pair(lat, lon)
        } else {
            null
        }
    }

    fun String.extractLatLon2(): Pair<Double, Double>? {
        val lat = "mlat=([\\d.]+)".toRegex().find(this)?.groupValues?.get(1)?.toDoubleOrNull()
        val lon = "mlon=([\\d.]+)".toRegex().find(this)?.groupValues?.get(1)?.toDoubleOrNull()
        return if (lat != null && lon != null) lat to lon else null
    }

    fun String.extractMessageAndImage(): Pair<String?, String?> {
        val doc = Jsoup.parse(this)
        val message = doc.select("p").first()?.text()
        val imageSrc = doc.select("img").first()?.attr("src")
        return Pair(message, imageSrc)
    }

    fun String.extractId(): String {
        val pattern =
            Regex("/chat/dialog/(attach|download)/\\d+/(\\d+)|/chat/dialog/(attach|download)/(\\d+)")
        val matchResult = pattern.find(this)
        return matchResult?.groups?.get(2)?.value ?: matchResult?.groups?.get(4)?.value ?: ""
    }

    inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayList(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayList(key)
        }
    }

    private fun convertUriToBase64(context: Context, uri: Uri): String {
        val inputStream: InputStream? =context.contentResolver.openInputStream(uri)
        return inputStream?.use {
            Base64.encodeToString(it.readBytes(), Base64.DEFAULT)
        } ?: ""
    }

}