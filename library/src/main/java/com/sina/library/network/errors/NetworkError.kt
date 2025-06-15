/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.errors

import com.google.gson.annotations.SerializedName
import com.sina.library.network.status.NetworkStatus

data class NetworkError(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    val data: NetworkStatus
)
