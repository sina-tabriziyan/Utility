package com.sina.library.network.web
import com.sina.library.data.model.HtmlLink
import java.util.regex.Pattern

// Object to extract HTML links from a given HTML content
object HTMLLinkExtractor {
    // Pattern to match <a> tags
    private val patternTag: Pattern = Pattern.compile("(?i)<a([^>]+)>(.+?)</a>")
    // Pattern to match href attributes within <a> tags
    private val patternLink: Pattern = Pattern.compile("\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))")

    /**
     * Extracts HTML links and their text from the provided HTML content.
     *
     * @param html The HTML content to parse.
     * @return A list of HtmlLink objects containing links and their corresponding text.
     */
    fun grabHTMLLinks(html: String): List<HtmlLink> {
        val result = mutableListOf<HtmlLink>()
        val matcherTag = patternTag.matcher(html)

        while (matcherTag.find()) {
            val href = matcherTag.group(1) // href attribute
            val linkText = matcherTag.group(2) // Link text

            val matcherLink = patternLink.matcher(href)
            if (matcherLink.find()) {
                val link = matcherLink.group(1).replace("'", "").replace("\"", "") // Cleaned link
                result.add(HtmlLink(link, linkText))
            }
        }
        return result
    }
}