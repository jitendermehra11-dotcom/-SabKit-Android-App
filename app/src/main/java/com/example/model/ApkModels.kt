package com.example.model

import android.graphics.drawable.Drawable

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val isSystemApp: Boolean,
    val permissions: List<String>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val signatures: List<String>,
    val icon: Drawable? = null
)
