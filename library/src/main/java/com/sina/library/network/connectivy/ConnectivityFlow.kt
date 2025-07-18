/**
 * Created by ST on 6/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.network.connectivy

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@SuppressLint("MissingPermission")

class ConnectivityFlow(context: Context, private val urlProvider: () -> String) :
    ConnectivityManager.NetworkCallback() {

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var internetCheckJob: Job? = null

    init {
        connectivityManager.registerDefaultNetworkCallback(this)

        CoroutineScope(Dispatchers.IO).launch {
            _isNetworkAvailable.value = checkInternetAccess()
            startInternetCheck()
        }
    }

    override fun onAvailable(network: Network) {
        CoroutineScope(Dispatchers.IO).launch {
            val isConnected = checkInternetAccess()
            _isNetworkAvailable.value = isConnected
            Log.d("ConnectivityFlow", "Network Available: $isConnected")
            startInternetCheck()
        }
    }

    override fun onLost(network: Network) {
        CoroutineScope(Dispatchers.IO).launch {
            _isNetworkAvailable.value = false
            Log.d("ConnectivityFlow", "Network Lost: false")
            stopInternetCheck()
        }
    }

    private fun startInternetCheck() {
        if (internetCheckJob?.isActive == true) return

        internetCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(15_000) // every 15 seconds
                val isConnected = checkInternetAccess()
                if (_isNetworkAvailable.value != isConnected) {
                    _isNetworkAvailable.value = isConnected
                    Log.d("ConnectivityFlow", "Updated Internet Status: $isConnected")
                }
            }
        }
    }

    private fun stopInternetCheck() {
        internetCheckJob?.cancel()
    }

    private suspend fun checkInternetAccess(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val urlc = URL(urlProvider()).openConnection() as HttpURLConnection
                urlc.connectTimeout = 3000
                urlc.connect()
                val isConnected = urlc.responseCode == 200
                urlc.disconnect()
                Log.d("ConnectivityFlow", "Internet Check Result: $isConnected")
                isConnected
            } catch (e: IOException) {
                Log.e("ConnectivityFlow", "Internet Check Failed: \${e.message}")
                false
            }
        }
    }
}