package com.example.model

data class AdMobTestUnit(
    val format: String,
    val unitId: String,
    val description: String,
    val recommendedSize: String
)

data class DeviceSpec(
    val title: String,
    val value: String
)
