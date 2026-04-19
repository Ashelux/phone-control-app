package com.phonecontrol.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.phonecontrol.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Logger.i("权限已授予")
        } else {
            Logger.w("部分权限被拒绝")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ConfigManager.init(this)
        setupUI()
        setupListeners()
        requestPermissions()
        checkInitialStatus()

        // 设置日志回调
        Logger.onLogUpdated = { logs ->
            runOnUiThread {
                binding.tvLogs.text = logs.joinToString("\n")
            }
        }

        Logger.i("手机控制助手已启动")
    }

    private fun setupUI() {
        // 加载保存的配置
        val config = ConfigManager.getConfig()
        binding.editServerIp.setText(config.serverIp)
        binding.editServerPort.setText(config.serverPort.toString())
        binding.editToken.setText(config.token)
        binding.editDeviceName.setText(config.deviceName)
    }

    private fun setupListeners() {
        binding.btnSaveConfig.setOnClickListener {
            saveConfig()
        }

        binding.btnCheckStatus.setOnClickListener {
            checkStatus()
        }

        binding.btnInstallDeps.setOnClickListener {
            installDeps()
        }

        binding.btnStartFrp.setOnClickListener {
            startFrp()
        }

        binding.btnStartClient.setOnClickListener {
            startClient()
        }

        binding.btnStopClient.setOnClickListener {
            stopClient()
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun saveConfig() {
        val serverIp = binding.editServerIp.text.toString().trim()
        val serverPort = binding.editServerPort.text.toString().toIntOrNull() ?: 5443
        val token = binding.editToken.text.toString().trim()
        val deviceName = binding.editDeviceName.text.toString().trim()

        if (serverIp.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "请填写服务器IP和Token", Toast.LENGTH_SHORT).show()
            return
        }

        val config = ConfigManager.Config(
            serverIp = serverIp,
            serverPort = serverPort,
            token = token,
            deviceName = deviceName
        )

        ConfigManager.saveConfig(config)
        Logger.i("配置已保存: $serverIp:$serverPort")
        Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
    }

    private fun checkStatus() {
        lifecycleScope.launch {
            try {
                // 检查 Termux
                val termuxInstalled = TermuxInstaller.isTermuxInstalled(this@MainActivity)
                updateTermuxStatus(termuxInstalled)

                // 检查 FRP
                val frpInstalled = FRPManager.isFRPInstalled()
                updateFrpcStatus(frpInstalled)

                Logger.i("状态检查完成")
            } catch (e: Exception) {
                Logger.e("状态检查失败", e)
            }
        }
    }

    private fun updateTermuxStatus(installed: Boolean) {
        binding.tvTermuxStatus.apply {
            text = if (installed) getString(R.string.termux_installed) else getString(R.string.termux_not_found)
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (installed) R.color.success else R.color.error
                )
            )
        }
    }

    private fun updateFrpcStatus(installed: Boolean) {
        binding.tvFrpcStatus.apply {
            text = if (installed) getString(R.string.frpc_installed) else getString(R.string.frpc_not_installed)
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (installed) R.color.success else R.color.warning
                )
            )
        }
    }

    private fun updateClientStatus(running: Boolean) {
        binding.tvClientStatus.apply {
            text = if (running) getString(R.string.client_running) else getString(R.string.client_stopped)
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (running) R.color.success else R.color.text_secondary
                )
            )
        }
        binding.btnStopClient.isEnabled = running
        binding.btnStartClient.isEnabled = !running
    }

    private fun checkInitialStatus() {
        checkStatus()
        updateClientStatus(false)
    }

    private fun installDeps() {
        if (!TermuxInstaller.isTermuxInstalled(this)) {
            // 打开 F-Droid 下载页面
            try {
                startActivity(Intent(Intent.ACTION_VIEW, TermuxInstaller.getFDFroidUri()))
                Logger.w("请安装 Termux 后继续")
            } catch (e: Exception) {
                Logger.e("无法打开下载页面", e)
            }
            return
        }

        lifecycleScope.launch {
            val result = TermuxInstaller.installDeps(this@MainActivity) { status ->
                Logger.i(status)
            }

            if (result.success) {
                // 打开 Termux
                startActivity(TermuxInstaller.getTermuxIntent())
                Logger.i("请在 Termux 中运行: ${TermuxInstaller.getInstallCommand()}")
            } else {
                Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFrp() {
        if (!ConfigManager.isConfigured()) {
            Toast.makeText(this, "请先保存服务器配置", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = FRPManager.downloadAndInstall { status ->
                Logger.i(status)
            }

            if (result.success) {
                // 打开 Termux 并显示启动命令
                startActivity(TermuxInstaller.getTermuxIntent())
                Logger.i("FRP 配置完成")
                Logger.i("在 Termux 中运行: ${FRPManager.getStartCommand()}")
                updateFrpcStatus(true)
            } else {
                Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startClient() {
        if (!ConfigManager.isConfigured()) {
            Toast.makeText(this, "请先保存服务器配置", Toast.LENGTH_SHORT).show()
            return
        }

        if (!FRPManager.isFRPInstalled()) {
            Toast.makeText(this, "请先配置 FRP", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // 打开 Termux
            startActivity(TermuxInstaller.getTermuxIntent())
            Logger.i("客户端启动中...")
            Logger.i("在 Termux 中运行以下命令:")

            val script = FRPManager.getClientScript()
            Logger.i("---")
            Logger.i(script)
            Logger.i("---")

            // 启动前台服务
            ControlService.start(this@MainActivity)
            updateClientStatus(true)
        }
    }

    private fun stopClient() {
        lifecycleScope.launch {
            Logger.i("停止客户端...")
            // 打开 Termux 执行停止命令
            startActivity(TermuxInstaller.getTermuxIntent())
            Logger.i("在 Termux 中运行: ${FRPManager.getStopCommand()}")

            ControlService.stop(this@MainActivity)
            updateClientStatus(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.onLogUpdated = null
    }
}
