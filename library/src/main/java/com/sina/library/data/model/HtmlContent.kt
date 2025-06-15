package com.sina.library.data.model
/**
 * Data model for storing structured HTML content.
 */
data class HtmlContent(
    val title: String = "",
    val textBlocks: List<String> = listOf(),
    val images: List<String> = listOf(),
    val links: List<String> = listOf(),
    val phoneNumbers: List<String> = listOf(),
    val mapLocations: List<String> = listOf(),
    val metadata: Map<String, String> = mapOf()
)