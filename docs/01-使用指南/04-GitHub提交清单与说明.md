# 04 — GitHub 提交清单与说明

> **组员完整流程（Fork → 分支 → PR → 发组长）** → **[06-GitHub傻瓜式提交全流程.md](06-GitHub傻瓜式提交全流程.md)**  
> **只提交这个文件夹**：`D:\SchoolWorkPlace\Database\CSRRMupdate`  
> **不要提交**：兄弟目录 `CampusStudyRoomReservationManagementSystem-master`（那是 V1.0 原型包）  
> **仓库建议名**：`campus-study-room-reservation`（或按老师要求命名）

---

## 你要交什么？（一句话）

把 **能运行的项目 + 文档 + 已打包的前端** 推到 GitHub；**密码、编译缓存、node_modules 不要交**。

---

## 第 0 步：安装 Git（你电脑目前还没有）

在 PowerShell 里输入 `git --version`，若提示「无法识别 git」，先装 Git：

1. 打开 [Git for Windows 下载页](https://git-scm.com/download/win)  
2. 一路 **Next**（默认选项即可）  
3. **关掉所有 PowerShell 窗口，重新打开一个新的**  
4. 再执行：

```powershell
git --version
```

应显示类似 `git version 2.x.x`。

---

## 第 1 步：注册 GitHub 并新建空仓库

1. 打开 [https://github.com](https://github.com) 注册/登录  
2. 右上角 **+** → **New repository**  
3. 填写：
   - **Repository name**：例如 `campus-study-room-reservation`
   - **Public** 或 **Private**（按课程要求）
   - **不要勾选**「Add a README file」（本地已有 README，避免冲突）
   - **不要勾选**「Add .gitignore」（项目里已有）
4. 点 **Create repository**  
5. 创建完成后，复制页面上的 **HTTPS 地址**，形如：

```text
https://github.com/你的用户名/campus-study-room-reservation.git
```

先记在记事本里，第 6 步要用。

> **推送时要登录**：GitHub 已不支持用账号密码 push，需用 **Personal Access Token（PAT）** 当密码。  
> 生成路径：GitHub → 头像 → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)** → **Generate new token**，勾选 **repo**，复制 token（只显示一次，请保存）。

---

## 第 2 步：提交前本地检查（必做）

打开 **PowerShell**，整段复制执行：

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate

# 2.1 确认密码文件存在但不会被提交（正常）
Test-Path "src\main\resources\application-local.properties"

# 2.2 跑测试 + 打包前端（约 1～3 分钟）
.\mvnw.cmd test
.\scripts\build-frontend.ps1
```

两项都成功后再继续。

---

## 第 3 步：第一次初始化 Git 仓库

仍在项目根目录：

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate

git init
git branch -M main
```

---

## 第 4 步：检查「会不会误交密码」（最重要）

```powershell
git status
```

### 你应该看到（绿色/未跟踪，可以交）

- `README.md`、`pom.xml`、`src/`、`frontend/`、`docs/`、`scripts/` 等

### 你绝对不能看到（若出现 = 危险）

| 若 status 里出现 | 说明 | 处理 |
|------------------|------|------|
| `application-local.properties` | 含 MySQL 密码 | **不要** `git add` 它；确认 `.gitignore` 第 11 行有该文件名 |
| `target/` | 编译产物 | 不应被 add（已在 ignore） |
| `frontend/node_modules/` | 依赖包 | 不应被 add |
| `uploads/` 里除 README 外的文件 | 运行时上传 | 不应被 add |

**安全自检命令**（复制执行，应无输出或只提示 ignored）：

```powershell
git check-ignore -v src\main\resources\application-local.properties
git check-ignore -v target
git check-ignore -v frontend\node_modules
```

第一行应显示被 `.gitignore` 忽略。

---

## 第 5 步：添加文件并提交

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate

git add README.md pom.xml mvnw mvnw.cmd .gitignore .mvn
git add src frontend docs scripts uploads/README.md

# 再次确认：staging 里不应有 application-local.properties
git status
```

`git status` 在 **Changes to be committed** 区域**不应**出现 `application-local.properties`。

确认无误后提交：

```powershell
git commit -m "feat: 校园自习室预约系统 V1.1 完整交付（前后端+文档+UI对齐）"
```

若提示需要设置用户名邮箱（首次用 Git），先执行（改成你的信息）：

```powershell
git config user.name "你的姓名或GitHub用户名"
git config user.email "你的邮箱@example.com"
```

然后再执行一次 `git commit ...`。

---

## 第 6 步：关联 GitHub 并推送

把下面命令里的 URL **换成你在第 1 步复制的地址**：

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate

git remote add origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

- **Username**：你的 GitHub 用户名  
- **Password**：粘贴 **PAT token**（不是 GitHub 登录密码）

推送成功后会显示 `main -> main` 之类字样。

### 若 `remote origin already exists`

```powershell
git remote set-url origin https://github.com/你的用户名/你的仓库名.git
git push -u origin main
```

### 若 push 被拒绝（远程已有 README）

说明创建仓库时勾选了 README。在 GitHub 网页删掉远程 README 或本地执行：

```powershell
git pull origin main --allow-unrelated-histories
# 若有冲突，保留本地 README，再：
git push -u origin main
```

---

## 第 7 步：在 GitHub 网页上确认

打开你的仓库页面，应能看到：

- 根目录有 `README.md`、`pom.xml`、`frontend/`、`docs/`  
- **没有** `application-local.properties`  
- **没有** `node_modules/`、`target/`  
- 点进 `docs/01-使用指南/` 能看到答辩清单、启动流程等  

---

## 第 8 步：模拟老师 clone 验收（强烈建议）

另开一个目录测试「别人能不能跑起来」：

```powershell
cd D:\
git clone https://github.com/你的用户名/你的仓库名.git CSRRMupdate-test
cd CSRRMupdate-test

Copy-Item "src\main\resources\application-local.properties.example" "src\main\resources\application-local.properties"
notepad "src\main\resources\application-local.properties"
# 填好 MySQL 密码后保存

.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

浏览器 **http://localhost:8080**，用学生账号 `202301010101` / `123456` 登录。  
能登录 = 提交合格。

---

## 附录 A · 必须提交 vs 禁止提交（对照表）

### ✅ 必须提交

| 类别 | 路径 |
|------|------|
| 后端 | `src/main/java/**`、`src/test/**` |
| 配置 | `application.properties`、`application-local.properties.example`（**只有 example**） |
| 前端产物 | `src/main/resources/static/**`（含 `index.html`） |
| 前端源码 | `frontend/src/**`、`package.json`、`package-lock.json`、`vite.config.js` 等 |
| 构建 | `pom.xml`、`mvnw`、`mvnw.cmd`、`.mvn/` |
| 文档 | `docs/**`、`README.md` |
| 脚本 | `scripts/*.ps1` |
| 占位 | `uploads/README.md` |

### ❌ 禁止提交

| 路径 | 原因 |
|------|------|
| `application-local.properties` | **真实 MySQL 密码** |
| `target/` | Maven 编译输出 |
| `frontend/node_modules/` | 体积大，`npm install` 可恢复 |
| `uploads/*`（除 README） | 运行时上传文件 |
| `.idea/`、`.vscode/` | IDE 个人配置 |
| 课程 Word 报告、PPT、视频 | 通常单独交，不进 Git |

---

## 附录 B · 提交信息模板（可选）

```
feat: CSRRM V1.1 — 预约/签到/暂离/管理端/UI原型对齐

- Spring Boot 3.5 + Vue 3 + MySQL 8
- 含完整 docs、答辩验收清单、UI 对齐文档
- 前端已 build 至 static，clone 后 mvnw spring-boot:run 即可演示
```

---

## 附录 C · 课程除 GitHub 外还可能要交

| 材料 | 是否进 Git |
|------|:---:|
| 本仓库（可运行项目） | ✅ |
| 数据库课程设计报告 Word | ❌ 单独交 |
| 答辩 PPT / 演示视频 | ❌ 单独交 |

---

## 附录 D · 相关文档

- 启动命令：[05-启动流程与PowerShell命令.md](05-启动流程与PowerShell命令.md)  
- 提交前自检：[03-提交前全面自检清单.md](03-提交前全面自检清单.md)  
- 答辩流程：[02-答辩验收清单.md](02-答辩验收清单.md)

---

## 你现在的进度（对照打勾）

| 步骤 | 内容 | 你完成了吗 |
|:---:|------|:---:|
| 0 | 安装 Git，`git --version` 有输出 | ☐ |
| 1 | GitHub 建好空仓库，复制 HTTPS URL | ☐ |
| 2 | `mvnw test` + `build-frontend.ps1` 成功 | ☐ |
| 3 | `git init` + `git branch -M main` | ☐ |
| 4 | `git status` 里没有密码文件 | ☐ |
| 5 | `git add` + `git commit` | ☐ |
| 6 | `git remote` + `git push`（PAT 当密码） | ☐ |
| 7 | GitHub 网页能看到完整目录 | ☐ |
| 8 | 另目录 `git clone` 能跑 8080 | ☐ |
