package com.sina.library.data.model

data class ScreenShot(
    val width: Int = 300,
    val height: Int = 200,
    val hCenterPercent: Float = 0.5f,
    val vCenterPercent: Float = 0.5f,
    val rootFolder: String = "Teamayr",
    val childFolder: String = "Teamyar Images",
    val latitude: Double?,
    val longitude: Double?
)