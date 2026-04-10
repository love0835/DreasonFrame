# DreasonFrame — Android 地區流量自動分流

一款 Android VPN 分流工具，自動將手機流量按地區路由：大陸 App/域名走大陸代理、台灣 App/域名走台灣代理（或直連）、其餘直連。

## 功能特色

- **自動分流** — 根據 App、域名、IP 段自動決定流量走向
- **Xray 協議** — 支援 VMess、VLESS、Trojan（主流協議）
- **多種傳輸** — TCP / WebSocket / gRPC / HTTP/2 / QUIC
- **TLS/Reality** — 支援 TLS + 指紋偽裝、Reality 協議
- **一鍵匯入** — 直接貼上 `vmess://` `vless://` `trojan://` 分享連結
- **剪貼簿匯入** — 批次匯入多個伺服器
- **未設代理自動直連** — 沒設台灣代理？流量自動走手機本身網路
- **內建規則** — 預載中國/台灣常用域名和 IP 段
- **連線日誌** — 即時查看每個連線的路由決策

## 支援的協議

| 協議 | 說明 |
|------|------|
| **VLESS** | 輕量主流協議，支援 xtls-rprx-vision |
| **VMess** | V2Ray 經典協議 |
| **Trojan** | 偽裝 HTTPS 流量 |
| **Shadowsocks** | 傳統代理協議 |
| **SOCKS5** | 通用代理 |
| **HTTP** | HTTP CONNECT 代理 |

## 路由規則（優先順序）

1. **App 規則** — 指定某個 App 走哪條線路（如：Bilibili → 中國代理）
2. **域名規則** — 精確匹配、後綴匹配（`*.baidu.com`）、關鍵字匹配
3. **IP/CIDR 規則** — 根據目標 IP 段判斷（內建 APNIC 資料）
4. **預設** — 以上都不匹配時走直連

## 架構

```
[手機 App] → [TUN 介面] → [DNS 攔截 + FakeIP] → [tun2socks]
                                                      ↓
                                            [Local SOCKS5 Dispatcher]
                                               ↓       ↓       ↓
                                          中國代理  台灣代理  直連
```

| 模組 | 說明 |
|------|------|
| `:app` | UI 層 — Jetpack Compose + Material 3 |
| `:core` | 資料層 — Room 資料庫、Repository、DataStore |
| `:tunnel` | VPN 引擎 — VpnService、DNS、路由引擎、代理客戶端 |

## 編譯

### 環境需求

- Android Studio 2024.2+ (Ladybug)
- JDK 17+（Android Studio 自帶）
- Android SDK API 35

### 步驟

```bash
git clone https://github.com/love0835/DreasonFrame.git
cd DreasonFrame
git checkout claude/regional-traffic-routing-EKzEP
```

用 Android Studio 開啟專案，等 Gradle Sync 完成後：

```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

APK 產出路徑：`app/build/outputs/apk/debug/app-debug.apk`

### 注意事項

如果遇到 Java 版本問題，確認使用 Android Studio 自帶的 JDK：

```bash
# Windows PowerShell
set JAVA_HOME=C:\Users\你的帳號\AppData\Local\Google\AndroidStudio版本\jbr
.\gradlew.bat assembleDebug
```

## 使用方式

1. 安裝 APK 到手機
2. 開啟 App → **伺服器** → 匯入你的代理伺服器連結（vmess:// / vless:// / trojan://）
3. 選擇路由分組（中國代理 / 台灣代理）
4. 回到首頁 → 按下 VPN 開關
5. 流量自動分流！

> 沒設台灣代理的話，台灣流量會自動走手機本身的網路，不需要額外設定。

## 技術細節

- **FakeIP DNS** — 攔截 DNS 分配假 IP，反查域名做路由判斷
- **Trie 域名匹配** — 反轉標籤 Trie 樹，高效匹配萬級域名規則
- **Radix Trie CIDR** — Patricia Trie 匹配數千個 IP 段
- **hev-socks5-tunnel** — 原生 C 層 TUN→SOCKS5 轉換
- **XrayConfigGenerator** — 自動生成 Xray-core JSON 配置
- **VpnService.protect()** — 直連流量繞過 TUN 防止迴圈

## License

MIT
