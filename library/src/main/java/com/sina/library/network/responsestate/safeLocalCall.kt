package com.sina.library.network.responsestate

import java.io.FileNotFoundException
import java.io.IOException

inline fun <T> safeLocalCall(block: () -> T): Result<T, DataError.Local> {
    return try {
        Result.Success(block())
    } catch (e: IOException) {
        Result.Error(DataError.Local.READ_ERROR)
    } catch (e: SecurityException) {
        Result.Error(DataError.Local.PERMISSION_DENIED)
    } catch (e: FileNotFoundException) {
        Result.Error(DataError.Local.FILE_NOT_FOUND)
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }
}