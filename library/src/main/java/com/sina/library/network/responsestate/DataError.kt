package com.sina.library.network.responsestate

import android.content.Context
import androidx.annotation.StringRes
import com.sina.library.network.responsestate.UiText.*
import com.sina.library.utility.R

sealed interface DataError : Error {
    enum class Network : DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERIALIZATION,
        UNKNOWN
    }

    enum class Local : DataError {
        DISK_FULL,
        FILE_NOT_FOUND,
        INVALID_PATH,
        READ_ERROR,
        WRITE_ERROR,
        UNSUPPORTED_FORMAT,
        CACHE_CORRUPTED,
        PERMISSION_DENIED, UNKNOWN

    }
}

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(
        @StringRes val id: Int,
        val args: Array<Any> = arrayOf()
    ) : UiText()

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(id, *args)
        }
    }
}
fun DataError.asUiText(): UiText {
    return when (this) {
        DataError.Network.REQUEST_TIMEOUT -> StringResource(R.string.the_request_timed_out)
        DataError.Network.TOO_MANY_REQUESTS -> StringResource(R.string.youve_hit_your_rate_limit)
        DataError.Network.NO_INTERNET -> StringResource(R.string.no_internet)
        DataError.Network.PAYLOAD_TOO_LARGE -> StringResource(R.string.file_too_large)
        DataError.Network.SERVER_ERROR -> StringResource(R.string.server_error)
        DataError.Network.SERIALIZATION -> StringResource(R.string.error_serialization)
        DataError.Network.UNKNOWN -> StringResource(R.string.unknown_error)
        DataError.Local.DISK_FULL -> StringResource(R.string.error_disk_full)
        DataError.Local.FILE_NOT_FOUND -> StringResource(R.string.error_file_not_found)
        DataError.Local.INVALID_PATH -> StringResource(R.string.error_invalid_path)
        DataError.Local.READ_ERROR -> StringResource(R.string.error_read_error)
        DataError.Local.WRITE_ERROR -> StringResource(R.string.error_write_error)
        DataError.Local.UNSUPPORTED_FORMAT -> StringResource(R.string.error_unsupported_format)
        DataError.Local.CACHE_CORRUPTED -> StringResource(R.string.error_cache_corrupted)
        DataError.Local.PERMISSION_DENIED -> StringResource(R.string.error_permission_denied)
        DataError.Local.UNKNOWN -> StringResource(R.string.unknown_error)
    }
}
fun Result.Error<*, DataError>.asErrorUiText(): UiText {
    return error.asUiText()
}
