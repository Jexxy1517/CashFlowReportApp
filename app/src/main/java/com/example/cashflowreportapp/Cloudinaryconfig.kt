package com.example.cashflowreportapp

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryConfig {

    private var configured = false

    fun init(context: Context) {
        if (configured) return

        val config: MutableMap<String, String> = HashMap()
        config["cloud_name"] = "dzb6g2btk"
        config["api_key"] = "612725397824513"
        config["api_secret"] = "c_5JmZpv9_GlOeFrVEnIm6bJuqo"
        config["secure"] = "true"

        MediaManager.init(context, config)
        configured = true
    }
}