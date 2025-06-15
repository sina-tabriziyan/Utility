package com.sina.library.network.responses

import com.sina.library.network.status.Status
import com.sina.library.network.status.NetworkStatus

fun NetworkStatus.asStatus() = Status(status)
