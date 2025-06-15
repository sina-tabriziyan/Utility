/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.responses

import com.sina.library.network.states.ResponseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> Flow<ResponseState<T>>.asSafeOpen(): Flow<T> = map { response ->
    return@map when (response) {
        is ResponseState.Error -> throw response.exception!!
        is ResponseState.Success -> response.data
    }
}