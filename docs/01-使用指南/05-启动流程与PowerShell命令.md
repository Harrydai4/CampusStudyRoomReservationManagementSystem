# 05 — 启动流程与 PowerShell 命令（可复制）

> **项目根目录（所有命令都从这里或子目录执行）**：`D:\SchoolWorkPlace\Database\CSRRMupdate`  
> **答辩/验收推荐地址**：[http://localhost:8080](http://localhost:8080)  
> **改 UI 时开发地址**：[http://localhost:5173](http://localhost:5173)（需同时开着 8080 后端）

---

## 0. 启动前要有的东西


| 组件    | 要求          | 检查命令（复制到 PowerShell）                  |
| ----- | ----------- | ------------------------------------- |
| JDK   | 21 及以上      | `java -version`                       |
| MySQL | 8.x 服务已运行   | `Get-Service *mysql`* 或任务管理器看 MySQL80 |
| Node  | 18+（改前端才必须） | `node -v`                             |


一键环境检查（在项目根目录）：

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\check-env.ps1
```

---

## 1. 首次使用：配置数据库密码（只做一次）

### 方式 A — 推荐：本地配置文件（密码不进 Git）

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
Copy-Item "src\main\resources\application-local.properties.example" "src\main\resources\application-local.properties"
notepad "src\main\resources\application-local.properties"
```

在记事本里把 `spring.datasource.password=在这里填写你的MySQL密码` 改成你的 root 密码，保存关闭。

> `application-local.properties` 已在 `.gitignore` 中，**不要提交到 GitHub**。

### 方式 B — 临时：当前窗口环境变量（关掉 PowerShell 就失效）

```powershell
$env:DB_PASSWORD="你的MySQL密码"
```

### root 无密码

若 MySQL root 就是空密码，可跳过第 1 节，直接看第 2 节启动后端。

---

## 2. 启动 MySQL（二选一）

### 2A. 本机已安装 MySQL（最常见）

1. 打开「服务」→ 找到 **MySQL80**（或类似名称）→ 状态为 **正在运行**。
2. 若未运行，在**管理员 PowerShell** 中：

```powershell
Start-Service MySQL80
```

（服务名以你机器为准，可用 `Get-Service *mysql*` 查看。）

### 2B. 用 Docker 启动 MySQL（免本机安装）

需已安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)。

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate\docs\06-部署配置
docker compose up -d
```

默认 root 密码为 `**csrrm_dev_123**`（见 `docker-compose.yml`）。随后任选其一：

```powershell
# 环境变量（当前窗口有效）
$env:DB_PASSWORD="csrrm_dev_123"
```

或写入 `application-local.properties` 的 `spring.datasource.password=csrrm_dev_123`。

---

## 3. 三种启动场景（选一种即可）

### 场景一：答辩 / 验收 — 只开 8080（推荐）

前端已打包进后端，**只需一个 PowerShell 窗口**。

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
# 若用方式 B 配密码，先执行： $env:DB_PASSWORD="你的密码"
.\mvnw.cmd spring-boot:run
```

**成功标志**（日志里出现）：

```text
Tomcat started on port 8080
Started CampusStudyRoomReservationManagementSystemApplication
```

浏览器打开：**[http://localhost:8080](http://localhost:8080)** → 应看到登录页（蓝紫渐变）。  
建议 **Ctrl+F5** 强刷，避免浏览器缓存旧页面。

停止后端：在该窗口按 **Ctrl+C**。

---

### 场景二：日常改前端 — 8080 + 5173 两个窗口

**窗口 A（后端，不要关）：**

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\mvnw.cmd spring-boot:run
```

**窗口 B（前端开发服务器）：**

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate\frontend
npm install
npm run dev
```

浏览器打开：**[http://localhost:5173](http://localhost:5173)**（`/api` 会自动代理到 8080）。

> **常见错误**：在 `frontend` 目录下执行 `.\mvnw.cmd` 会失败。Maven 命令**必须在项目根目录**执行。

---

### 场景三：改完前端代码 — 重新打包到 8080

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\build-frontend.ps1
```

若 `npm run build` 报错，脚本会自动尝试备用 Vite 命令。完成后：

1. 若后端正在跑 → 窗口 A **Ctrl+C** 停掉；
2. 再执行：

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\mvnw.cmd spring-boot:run
```

访问 **[http://localhost:8080](http://localhost:8080)** 查看新界面。

---

## 4. 完整「从零到能登录」复制块（一条龙）

按顺序**整段复制**到 PowerShell 即可（把路径、密码改成你的）：

```powershell
# ---------- 第 1 步：进入项目 ----------
cd D:\SchoolWorkPlace\Database\CSRRMupdate

# ---------- 第 2 步：环境检查（可选） ----------
.\scripts\check-env.ps1

# ---------- 第 3 步：数据库密码（首次且 root 有密码时） ----------
# 若还没有 application-local.properties，取消下面两行注释并改密码：
# Copy-Item "src\main\resources\application-local.properties.example" "src\main\resources\application-local.properties"
# notepad "src\main\resources\application-local.properties"
# 或仅本窗口有效：
# $env:DB_PASSWORD="你的MySQL密码"

# ---------- 第 4 步：启动后端 ----------
.\mvnw.cmd spring-boot:run
```

后端起来后，**新开浏览器**访问：[http://localhost:8080](http://localhost:8080)  

**测试账号**：


| 角色    | 账号           | 密码       |
| ----- | ------------ | -------- |
| 学生    | 202301010101 | 123456   |
| 管理员   | admin        | admin123 |
| 超级管理员 | superadmin   | super123 |


> **登录不了？** 若提示「账号或密码错误」，多半是本地测试时改过密码。项目默认在**每次启动后端**时自动把上表演示账号密码恢复为表中值（`app.demo.sync-accounts-on-startup=true`）。请**重启** `spring-boot:run` 后再用 `123456` / `admin123` 登录。若仍失败，清空浏览器 Local Storage 中的 `token`、`role` 后重试。

---

## 5. 常用辅助命令

### 跑单元测试（不启动网站）

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\mvnw.cmd test
```

### 查 8080 是否被占用

```powershell
netstat -ano | findstr :8080
```

### 结束占用 8080 的 Java 进程（慎用，会关掉对应 Java）

```powershell
# 先看 PID，再替换下面的 12345
taskkill /PID 12345 /F
```

### 仅安装前端依赖（不启动）

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate\frontend
npm install
```

---

## 6. 启动失败对照


| 现象                                              | 原因               | 处理（复制命令/操作）                                                                 |
| ----------------------------------------------- | ---------------- | --------------------------------------------------------------------------- |
| `Communications link failure` / `Access denied` | MySQL 未开或密码错     | 开 MySQL 服务；检查 `application-local.properties` 或 `$env:DB_PASSWORD`           |
| `不支持发行版本 21`                                    | JDK 版本过低         | 安装 JDK 21+，设置 `JAVA_HOME` 后重开 PowerShell                                    |
| 在 `frontend` 下运行 `mvnw` 报错                      | 目录错了             | `cd D:\SchoolWorkPlace\Database\CSRRMupdate` 再 `.\mvnw.cmd spring-boot:run` |
| 8080 能开但 UI 很旧                                  | 未重新 build 前端     | `.\scripts\build-frontend.ps1` 后重启后端                                        |
| 8080 无法访问                                       | 后端未起来或端口占用       | 看日志是否 `Tomcat started`；`netstat` 查占用                                        |
| 登录 404                                          | 只开了 5173 但后端没开   | 先保证 8080 后端成功启动                                                             |
| 页面空白                                            | 未 build 且没用 5173 | 用场景二开 `npm run dev`，或场景三 `build-frontend`                                   |


---

## 7. 与答辩验收的关系

后端 + 浏览器走通后，按 **[02-答辩验收清单](02-答辩验收清单.md)** 逐项操作。  
交作业前可再跑 **[03-提交前全面自检清单](03-提交前全面自检清单.md)**。

---

## 8. 流程图（一眼看懂）

```text
MySQL 已启动
    ↓
配置密码（application-local.properties 或 DB_PASSWORD）
    ↓
cd CSRRMupdate 根目录
    ↓
.\mvnw.cmd spring-boot:run
    ↓
浏览器 http://localhost:8080
    ↓
学生/管理员登录验收
```

**改 UI 时**：在「启动后端」之后增加 `frontend` → `npm run dev` → 访问 5173。

---

## 9. 全组共用一套库（服务器管理员）

> 详细说明、排错、备份 → **[07-多人共用一套系统与数据库.md](07-多人共用一套系统与数据库.md)**

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate

# MySQL（Docker）
.\scripts\setup-shared-mysql-docker.ps1

# 配置（首次）
Copy-Item src\main\resources\application-shared.properties.example src\main\resources\application-shared.properties
notepad src\main\resources\application-shared.properties

# 防火墙（管理员 PowerShell）+ 启动
.\scripts\open-firewall-shared.ps1
.\scripts\start-shared-server.ps1
```

组员访问终端里显示的 `http://<服务器IP>:8080`，**不要**各自再跑 `spring-boot:run`。