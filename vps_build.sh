#!/bin/bash
# Android 编译环境一键安装脚本
# 在 Ubuntu 22.04 VPS 上运行

set -e

echo "========================================="
echo "  Android 编译环境安装"
echo "========================================="

# 1. 安装基础依赖
echo "[1/6] 安装基础依赖..."
apt update && apt upgrade -y
apt install -y openjdk-17-jdk wget unzip git curl

# 2. 设置 JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "JAVA_HOME=$JAVA_HOME" >> ~/.bashrc

# 3. 下载 Android SDK
echo "[2/6] 下载 Android SDK..."
mkdir -p /opt/android-sdk/cmdline-tools
cd /opt/android-sdk/cmdline-tools

# 下载 command line tools
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip

# 4. 安装 Android SDK 组件
echo "[3/6] 安装 Android SDK 组件..."
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" --sdk_root=$ANDROID_HOME

# 5. 克隆并编译项目
echo "[4/6] 克隆项目..."
cd ~
git clone https://github.com/Ashelux/phone-control-app.git
cd phone-control-app

# 6. 编译 APK
echo "[5/6] 编译 APK..."
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

chmod +x gradlew
# 内存优化: 2C4G 配置
export GRADLE_OPTS="-Xmx1536m -XX:+UseSerialGC"
./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m" --max-workers=1

# 7. 复制 APK 到 web 目录方便下载
echo "[6/6] 部署 APK..."
mkdir -p /var/www/html/apk
cp app/build/outputs/apk/debug/app-debug.apk /var/www/html/apk/

echo "========================================="
echo "  编译完成!"
echo "========================================="
echo ""
echo "APK 下载地址: http://你的服务器IP/apk/app-debug.apk"
echo "APK 位置: ~/phone-control-app/app/build/outputs/apk/debug/app-debug.apk"
