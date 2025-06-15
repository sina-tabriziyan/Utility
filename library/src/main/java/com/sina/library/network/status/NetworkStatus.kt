/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.status

import com.google.gson.annotations.SerializedName

data class NetworkStatus(
    @SerializedName("status") val status: Int
)