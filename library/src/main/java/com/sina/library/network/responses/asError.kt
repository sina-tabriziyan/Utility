/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.responses

import com.sina.library.network.errors.Error
import com.sina.library.network.errors.NetworkError

fun NetworkError.asError() = Error(
    code = code,
    message = message,
    data = data.asStatus()
)