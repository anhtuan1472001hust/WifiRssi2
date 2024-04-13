package com.example.rssiwifi

data class WifiModel(
    val wifiName: String? = null,
    val wifiMac: String? = null,
    var wifiRssi: Int? = null,
    var selected: Boolean = false,
)
