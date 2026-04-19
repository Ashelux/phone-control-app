package com.phonecontrol.app

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FRPManager {
    private const val FRP_VERSION = "0.58.0"
    private const val FRP_DIR = "/data/local/tmp/frp"
    private const val FRPC_BIN = "$FRP_DIR/frpc"
    private const val FRPC_CONFIG = "$FRP_DIR/frpc.ini"

    data class FRPResult(
        val success: Boolean,
        val message: String,
        val configPath: String = "",
        val binPath: String = ""
    )

    fun isFRPInstalled(): Boolean {
        return File(FRPC_BIN).exists()
    }

    fun getArch(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64"
    }

    fun getFRPArchiveName(): String {
        val arch = when {
            Build.CPU_ABI.contains("arm64") -> "arm64"
            Build.CPU_ABI.contains("armeabi") -> "arm"
            Build.CPU_ABI.contains("x86_64") -> "amd64"
            Build.CPU_ABI.contains("x86") -> "386"
            else -> "arm64"
        }
        return "frp_${FRP_VERSION}_linux_${arch}.tar.gz"
    }

    fun getDownloadURL(): String {
        return "https://github.com/fatedier/frp/releases/download/v0.58.0/${getFRPArchiveName()}"
    }

    suspend fun downloadAndInstall(onProgress: (String) -> Unit): FRPResult {
        return withContext(Dispatchers.IO) {
            try {
                val arch = getArch()
                onProgress("检测到 CPU 架构: $arch")

                val frpDir = File(FRP_DIR)
                if (!frpDir.exists()) {
                    frpDir.mkdirs()
                }

                // 生成配置文件的命令
                val configContent = buildFRPCConfig()
                val configFile = File(FRPC_CONFIG)
                configFile.writeText(configContent)

                Logger.i("FRP 配置已准备")
                Logger.d("配置路径: $FRPC_CONFIG")

                FRPResult(
                    success = true,
                    message = "FRP 已配置，请手动启动",
                    configPath = FRPC_CONFIG,
                    binPath = FRPC_BIN
                )
            } catch (e: Exception) {
                Logger.e("FRP 配置失败", e)
                FRPResult(false, "配置失败: ${e.message}")
            }
        }
    }

    fun buildFRPCConfig(): String {
        val config = ConfigManager.getConfig()
        return """
[common]
server_addr = ${config.serverIp}
server_port = ${config.serverPort}
token = ${config.token}
tls_enable = true

[phone_control]
type = tcp
local_ip = 127.0.0.1
local_port = 8000
remote_port = 7020
use_encryption = true
use_compression = true
"""
    }

    fun getStartCommand(): String {
        return "$FRPC_BIN -c $FRPC_CONFIG"
    }

    fun getStopCommand(): String {
        return "pkill -f frpc || true"
    }

    fun getClientScript(): String {
        val config = ConfigManager.getConfig()
        val deviceName = config.deviceName.ifEmpty { "android-${Build.SERIAL}" }
        val serverUrl = "http://${config.serverIp}:8000"

        return """
#!/bin/bash
# 手机控制客户端启动脚本

SERVER_URL_VALUE="$serverUrl"
DEVICE_NAME_VALUE="$deviceName"

cd ~

# 启动 FRP (后台)
nohup ~/frpc -c ~/.frpc.ini > ~/frpc.log 2>&1 &

# 等待 FRP 启动
sleep 2

# 启动 Python 客户端
cd ~/phone_control
nohup python3 main.py --server ${'$'}SERVER_URL_VALUE --name ${'$'}DEVICE_NAME_VALUE > ~/client.log 2>&1 &

echo "客户端已启动"
echo "日志: ~/client.log"
echo "FRP日志: ~/frpc.log"
"""
    }
}
