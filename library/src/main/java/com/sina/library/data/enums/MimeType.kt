/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.data.enums

enum class MimeType(val mimeType: String, val extension: String) {
    TEXT_PLAIN("text/plain", "txt"),
    TEXT_HTML("text/html", "html"),
    IMAGE_JPEG("image/jpeg", "jpeg"),
    IMAGE_PNG("image/png", "png"),
    IMAGE_GIF("image/gif", "gif"),
    APPLICATION_PDF("application/pdf", "pdf"),
    AUDIO_MPEG("audio/mpeg", "mp3"), // Added
    // Add more MIME types as needed
}