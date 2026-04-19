package com.phonecontrol.app

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

object ConfigManager {
    private const val PREFS_NAME = "phone_control_prefs"
    private const val KEY_SERVER_IP = "server_ip"
    private const val KEY_SERVER_PORT = "server_port"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_DEVICE_NAME = "device_name"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    data class Config(
        var serverIp: String = "",
        var serverPort: Int = 5443,
        var token: String = "",
        var deviceName: String = ""
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveConfig(config: Config) {
        prefs.edit().apply {
            putString(KEY_SERVER_IP, config.serverIp)
            putInt(KEY_SERVER_PORT, config.serverPort)
            putString(KEY_TOKEN, config.token)
            putString(KEY_DEVICE_NAME, config.deviceName)
            apply()
        }
    }

    fun getConfig(): Config {
        return Config(
            serverIp = prefs.getString(KEY_SERVER_IP, "") ?: "",
            serverPort = prefs.getInt(KEY_SERVER_PORT, 5443),
            token = prefs.getString(KEY_TOKEN, "") ?: "",
            deviceName = prefs.getString(KEY_DEVICE_NAME, "") ?: ""
        )
    }

    fun isConfigured(): Boolean {
        val config = getConfig()
        return config.serverIp.isNotEmpty() && config.token.isNotEmpty()
    }
}
