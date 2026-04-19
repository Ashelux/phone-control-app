package com.phonecontrol.app

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object Logger {
    private const val TAG = "PhoneControl"
    private val logs = CopyOnWriteArrayList<String>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    var onLogUpdated: ((List<String>) -> Unit)? = null

    fun d(message: String) {
        log("DEBUG", message)
    }

    fun i(message: String) {
        log("INFO", message)
    }

    fun w(message: String) {
        log("WARN", message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        log("ERROR", message)
        throwable?.let { Log.e(TAG, it.message, it) }
    }

    private fun log(level: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp][$level] $message"
        logs.add(logLine)
        Log.d(TAG, logLine)

        // 只保留最近100条日志
        while (logs.size > 100) {
            logs.removeAt(0)
        }

        onLogUpdated?.invoke(getLogs())
    }

    fun getLogs(): List<String> = logs.toList()

    fun clear() {
        logs.clear()
        onLogUpdated?.invoke(emptyList())
    }
}
