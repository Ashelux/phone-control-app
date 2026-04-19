package com.phonecontrol.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TermuxInstaller {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_API_PACKAGE = "com.termux.api"

    data class InstallResult(
        val success: Boolean,
        val message: String,
        val output: String = ""
    )

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getTermuxIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            setClassName(TERMUX_PACKAGE, "$TERMUX_PACKAGE.HomeActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getTermuxApiIntent(): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            setClassName(TERMUX_API_PACKAGE, "$TERMUX_API_PACKAGE.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getPlayStoreUri(): Uri {
        return Uri.parse("market://details?id=$TERMUX_PACKAGE")
    }

    fun getFDFroidUri(): Uri {
        return Uri.parse("https://f-droid.org/packages/$TERMUX_PACKAGE")
    }

    suspend fun installDeps(context: Context, onProgress: (String) -> Unit): InstallResult {
        return withContext(Dispatchers.IO) {
            try {
                // 检查 Termux 是否安装
                if (!isTermuxInstalled(context)) {
                    return@withContext InstallResult(false, "Termux 未安装，请先安装")
                }

                // 打开 Termux 执行命令
                onProgress("正在打开 Termux...")
                val intent = getTermuxIntent()
                context.startActivity(intent)

                // 生成安装脚本
                val script = buildInstallScript()
                onProgress("安装脚本已准备")
                Logger.i("Termux 依赖安装完成，用户需在 Termux 中确认")
                Logger.d("安装命令: $script")

                InstallResult(true, "请在 Termux 中运行安装命令", script)
            } catch (e: Exception) {
                Logger.e("安装依赖失败", e)
                InstallResult(false, "安装失败: ${e.message}")
            }
        }
    }

    private fun buildInstallScript(): String {
        return """
            # Termux 依赖安装脚本
            pkg update -y && pkg install -y python3 git curl wget && mkdir -p ~/phone_control && echo "依赖安装完成"
        """.trimIndent()
    }

    fun getInstallCommand(): String {
        return "pkg update -y && pkg install -y python3 git curl wget"
    }
}
