# 手机控制助手

安卓 APP，一键配置手机控制环境。

## 功能

- 服务器配置管理
- 一键安装 Termux 依赖
- FRP 客户端配置
- 控制客户端启动/停止
- 实时日志查看

## 构建

### 本地构建

```bash
./gradlew assembleDebug
```

APK 输出: `app/build/outputs/apk/debug/app-debug.apk`

### GitHub Actions 自动构建

1. Fork 本仓库
2. 在 Actions 页面查看构建状态
3. 构建完成后在 Releases 页面下载 APK

## 安装

1. 下载 APK 安装到手机
2. 打开 APP，配置服务器信息
3. 安装 Termux 环境
4. 配置并启动 FRP
5. 启动控制客户端

## 服务器要求

- FRP 服务端 (v0.58.0+)
- FastAPI 控制服务

## 截图

<!-- 截图待添加 -->

## License

MIT
