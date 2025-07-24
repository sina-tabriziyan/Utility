package com.sina.library.network.responsestate

sealed interface DataError: Error {
    enum class Network: DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        PAYLOAD_TOO_LARGE,
        SERVER_ERROR,
        SERIALIZATION,
        UNKNOWN
    }
    enum class Local: DataError {
        DISK_FULL,
        FILE_NOT_FOUND,
        INVALID_PATH,
        READ_ERROR,
        WRITE_ERROR,
        UNSUPPORTED_FORMAT,
        CACHE_CORRUPTED,
        PERMISSION_DENIED    }
}
