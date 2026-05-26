# 08 — 跨 WiFi / 跨网络共用与安全访问

> **问题**：同学不在同一 WiFi，无法访问 `http://192.168.x.x:8080`。  
> **原则**：只暴露 **Web 应用（8080）**，**绝不**把 MySQL、远程桌面、整个电脑暴露到公网。

---

## 1. 三种方案怎么选？

| 方案 | 适用场景 | 安全性 | 同学要装什么 |
|------|----------|--------|--------------|
| **A. Tailscale（推荐）** | 固定小组、答辩、长期联调 | ⭐⭐⭐ 最高（私有虚拟网，不公开到全网） | 安装 Tailscale 并加入你的组网 |
| **B. Cloudflare 隧道** | 临时演示、老师远程看、任意网络 | ⭐⭐ 中等（HTTPS 公网链接，仅转发 8080） | 只开浏览器，复制链接即可 |
| **C. 同一 WiFi / 实验室网** | 能连同一局域网 | ⭐⭐⭐ | 只开浏览器 |

**不要用**：路由器把 3306/3389/8080 全端口映射到公网（等于电脑裸奔）。

---

## 2. 安全底线（必读）

| 必须做 | 说明 |
|--------|------|
| ✅ 只隧道 **8080** | 使用 `start-public-tunnel.ps1`，目标固定 `http://127.0.0.1:8080` |
| ✅ MySQL 只本机 | 连接用 `127.0.0.1:3306`，**不要**在路由器上映射 3306 |
| ✅ 演示结束关隧道 | Cloudflare 窗口 `Ctrl+C`；Tailscale 可退出 |
| ✅ 演示账号 | 默认 `123456`/`admin123` 仅课程用；公网演示勿放真实隐私 |
| ❌ 禁止 | 映射 RDP(3389)、SMB(445)、MySQL(3306) 到公网 |

启动公网隧道前自动检查：

```powershell
.\scripts\check-public-exposure-safety.ps1
```

---

## 3. 方案 A：Tailscale（最推荐，不同 WiFi 也安全）

**原理**：你和组员加入同一个「虚拟局域网」，只有装了 Tailscale 的设备能访问，**不经过公网搜索**。

### 3.1 你（服务器）一次性准备

1. 安装 Tailscale：  
   `winget install --id Tailscale.Tailscale -e`  
   或 [https://tailscale.com/download/windows](https://tailscale.com/download/windows)

2. 登录 Tailscale（微信/Google/Microsoft 均可）

3. 初始化共用库（若未做过）：  
   `.\scripts\init-shared-mysql-local.ps1`

### 3.2 每次演示（两个窗口）

**窗口 A — 后端：**

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\start-shared-server.ps1
```

**窗口 B — 查组员访问地址：**

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\start-tailscale-team.ps1
```

会输出类似：`http://100.64.x.x:8080`（仅 Tailscale 内设备可开）。

### 3.3 组员操作

1. 安装 Tailscale（手机/电脑均可）
2. 你发 **Tailscale 邀请链接**（Tailscale 管理后台 → Users → Invite）
3. 组员接受邀请后，浏览器打开你给的 `http://100.x.x.x:8080`

---

## 4. 方案 B：Cloudflare Quick Tunnel（公网 HTTPS 链接）

**原理**：Cloudflare 给你一条 `https://xxx.trycloudflare.com`，外网只转发到你本机 **8080**，不开放路由器端口。

### 4.1 一次性安装

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\install-tunnel-tools.ps1
```

关闭并重新打开 PowerShell。

### 4.2 每次演示（两个窗口）

**窗口 A — 后端：**

```powershell
.\scripts\start-shared-server.ps1
```

**窗口 B — 公网隧道：**

```powershell
.\scripts\check-public-exposure-safety.ps1
.\scripts\start-public-tunnel.ps1
```

终端会出现一行：

```text
https://xxxx-xxxx.trycloudflare.com
```

**把这条 HTTPS 链接发给全组**（任意 WiFi、手机流量均可打开）。

### 4.3 注意

- 免费 Quick Tunnel **每次启动 URL 会变**
- 链接在隧道运行期间有效，**关掉窗口即失效**
- 知道链接的人都能尝试访问 → 仅用于课程演示，结束务必 `Ctrl+C`

---

## 5. 从零开机完整流程（跨 WiFi 版）

```text
【你 · 首次】
  init-shared-mysql-local.ps1
  install-tunnel-tools.ps1  或  安装 Tailscale

【你 · 每次答辩】
  窗口1: start-shared-server.ps1
  窗口2: start-tailscale-team.ps1   （推荐）
      或 start-public-tunnel.ps1    （要公网 HTTPS 时）

【组员】
  Tailscale 方案: 装 Tailscale → 打开 http://100.x.x.x:8080
  Cloudflare 方案: 只点你发的 https://....trycloudflare.com
```

---

## 6. 与「共用数据库」的关系

| 组件 | 位置 | 组员能否直连 |
|------|------|--------------|
| MySQL | 仅你电脑 `127.0.0.1:3306` | ❌ 不应直连 |
| Spring Boot | 你电脑 `8080` | ✅ 通过 Tailscale / 隧道访问网页 |
| 数据 | 共用库 `study` / `csrrm_shared_123` | 仅后端连接，组员走网页 API |

**全组只访问一个网址**，数据库自然统一。

---

## 7. 常见问题

| 现象 | 处理 |
|------|------|
| 隧道启动失败 | 先确认窗口 A 已 `Tomcat started on port 8080` |
| Tailscale 100.x 打不开 | 组员是否已加入同一 tailnet；后端是否在跑 |
| Cloudflare 链接很慢 | 正常；演示够用；结束关隧道 |
| 安全扫描警告 MySQL 0.0.0.0 | 见下节：MySQL 改只监听本机 |

### MySQL 仅本机监听（可选加固）

编辑 MySQL 配置 `my.ini`（路径因安装而异）：

```ini
[mysqld]
bind-address=127.0.0.1
```

重启 MySQL80 服务。共用库仍由本机 Spring Boot 访问，组员不受影响。

---

## 8. 相关脚本

| 脚本 | 作用 |
|------|------|
| `install-tunnel-tools.ps1` | 安装 cloudflared |
| `check-public-exposure-safety.ps1` | 公网暴露前安全检查 |
| `start-public-tunnel.ps1` | Cloudflare HTTPS 公网链接 |
| `start-tailscale-team.ps1` | 输出 Tailscale 组员 URL |
| `start-shared-server.ps1` | 启动共用后端 |
| `init-shared-mysql-local.ps1` | 初始化共用 MySQL |

局域网方案见 [07-多人共用一套系统与数据库.md](07-多人共用一套系统与数据库.md)。
