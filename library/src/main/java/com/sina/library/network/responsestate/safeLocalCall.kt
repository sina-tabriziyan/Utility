package com.sina.library.network.responsestate

inline fun <T> safeLocalCall(block: () -> T): Result<T, DataError.Local> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(DataError.Local.DISK_FULL)
    }
}