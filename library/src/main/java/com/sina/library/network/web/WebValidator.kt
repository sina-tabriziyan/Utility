/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.web

import java.util.regex.Pattern

object WebValidator {
    private const val GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF"
    private val IP_ADDRESS = Pattern.compile(
        "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                + "|[1-9][0-9]|[0-9]))"
    )
    private const val GOOD_GTLD_CHAR = "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
    const val GTLD = "[$GOOD_GTLD_CHAR]{2,63}";
    private const val IRI = "[$GOOD_IRI_CHAR]([$GOOD_IRI_CHAR\\-]{0,61}[$GOOD_IRI_CHAR]){0,1}";
    private const val HOST_NAME = "($IRI\\.)+$GTLD";
    private val DOMAIN_NAME = Pattern.compile("($HOST_NAME|$IP_ADDRESS)");
    val webValidPattern = Pattern.compile(
        "((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                + "(?:" + DOMAIN_NAME + ")"
                + "(?:\\:\\d{1,5})?)" // plus option port number
                + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~" // plus option query params
                + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                + "(?:\\b|$)"
    )
}