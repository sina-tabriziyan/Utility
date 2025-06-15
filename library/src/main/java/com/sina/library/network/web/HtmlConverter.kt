package com.sina.library.network.web

import android.util.Patterns
import com.sina.library.data.model.HtmlContent
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import org.jsoup.nodes.Document
/**
 * Converts an HTML file or URL into structured HtmlContent.
 */
object HtmlConverter {

    /**
     * Converts an HTML file into structured HtmlContent.
     * @param htmlFile The HTML file to parse.
     * @return HtmlContent containing extracted structured data.
     */
    fun fromFile(htmlFile: File): HtmlContent {
        return parseHtml(Jsoup.parse(htmlFile, "UTF-8"))
    }

    /**
     * Converts an HTML URL into structured HtmlContent.
     * @param url The URL to fetch and parse.
     * @return HtmlContent containing extracted structured data.
     */
    fun fromUrl(url: String): HtmlContent {
        return try {
            val document = Jsoup.connect(url).get()
            parseHtml(document)
        } catch (e: IOException) {
            e.printStackTrace()
            HtmlContent(title = "Error: Unable to fetch content")
        }
    }

    /**
     * Converts an HTML string into structured HtmlContent.
     * @param htmlContent The HTML content as a string.
     * @return HtmlContent containing extracted structured data.
     */
    fun fromString(htmlContent: String): HtmlContent {
        return parseHtml(Jsoup.parse(htmlContent))
    }

    /**
     * Parses a Jsoup Document and extracts structured content.
     * @param document The parsed Jsoup document.
     * @return HtmlContent with extracted elements.
     */
    private fun parseHtml(document: Document): HtmlContent {
        return HtmlContent(
            title = document.title(),
            textBlocks = extractText(document),
            images = extractImages(document),
            links = extractLinks(document),
            phoneNumbers = extractPhoneNumbers(document),
            mapLocations = extractMapLocations(document),
            metadata = extractMetadata(document)
        )
    }

    /** Extracts text blocks from an HTML document. */
    private fun extractText(doc: Document): List<String> {
        return doc.body()
            .select("p, h1, h2, h3, h4, h5, h6, span, div")
            .mapNotNull { it.text().takeIf { text -> text.isNotBlank() } }
    }

    /** Extracts image URLs from an HTML document. */
    private fun extractImages(doc: Document): List<String> {
        return doc.select("img")
            .mapNotNull { it.absUrl("src").takeIf { url -> url.isNotBlank() } }
    }

    /** Extracts links (URLs) from an HTML document. */
    private fun extractLinks(doc: Document): List<String> {
        return doc.select("a[href]")
            .mapNotNull { it.absUrl("href").takeIf { url -> url.isNotBlank() } }
    }

    /** Extracts phone numbers from an HTML document. */
    private fun extractPhoneNumbers(doc: Document): List<String> {
        return doc.text()
            .split(" ", "\n")
            .mapNotNull { word ->
                word.takeIf { Patterns.PHONE.matcher(it).matches() }
            }
    }

    /** Extracts map locations (Google Maps links) from an HTML document. */
    private fun extractMapLocations(doc: Document): List<String> {
        return doc.select("a[href]")
            .mapNotNull { link ->
                link.absUrl("href").takeIf { it.contains("maps.google.com") || it.contains("google.com/maps") }
            }
    }

    /** Extracts metadata like title, description, and keywords from an HTML document. */
    private fun extractMetadata(doc: Document): Map<String, String> {
        return mapOf(
            "title" to (doc.title().takeIf { it.isNotBlank() } ?: "No Title"),
            "description" to (doc.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
                ?: "No Description"),
            "keywords" to (doc.select("meta[name=keywords]").attr("content").takeIf { it.isNotBlank() }
                ?: "No Keywords")
        )
    }
}