# 校园自习室预约管理系统（CSRRMupdate）

> **唯一开发目录**：`D:\SchoolWorkPlace\Database\CSRRMupdate`  
> **版本**：V1.1 · Spring Boot 3.5 + Vue 3 + MySQL 8

---

## 新手入口

**零基础请先读** → [傻瓜式全局项目讲解](docs/01-使用指南/00-傻瓜式全局项目讲解.md)

**答辩验收清单** → [02-答辩验收清单](docs/01-使用指南/02-答辩验收清单.md)

**必做 vs 可选 vs 助手已完成** → [必做与分工说明](docs/01-使用指南/01-必做与分工说明.md)

**文档总索引** → [docs/README.md](docs/README.md)

**启动流程（可复制 PowerShell）** → [05-启动流程与PowerShell命令](docs/01-使用指南/05-启动流程与PowerShell命令.md)

**GitHub 组员提交（Fork + PR）** → [06-GitHub傻瓜式提交全流程](docs/01-使用指南/06-GitHub傻瓜式提交全流程.md)

---

## 项目结构（精简）

```
CSRRMupdate/
├── docs/           # 全部文档
├── scripts/        # 环境检查、前端构建脚本
├── frontend/       # Vue 3 前端源码
├── src/            # Spring Boot 后端源码
├── uploads/        # 运行时上传文件（自动生成）
├── README.md       # 本文件：快速启动
└── pom.xml         # Maven 配置
```

> **V1.1 前端已打包**：可直接 `spring-boot:run` 后访问 **[http://localhost:8080](http://localhost:8080)**，不必再开 5173（开发改 UI 时仍可用 `npm run dev`）。

详细说明 → [目录结构说明](docs/02-架构说明/02-目录结构说明.md)

---

## 环境要求


| 组件      | 版本  |
| ------- | --- |
| JDK     | 21+ |
| MySQL   | 8.x |
| Node.js | 18+ |


---

## 快速启动

> 详细说明、Docker MySQL、排错表 → **[05-启动流程与PowerShell命令.md](docs/01-使用指南/05-启动流程与PowerShell命令.md)**

### 答辩演示（只开 8080，复制即用）

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\check-env.ps1
# 首次且 root 有密码：复制 example 为 application-local.properties 并填写密码（见 05 文档第 1 节）
.\mvnw.cmd spring-boot:run
```

浏览器：**http://localhost:8080**（建议 Ctrl+F5 强刷）

### 改前端时（8080 + 5173 两个窗口）

**窗口 A — 后端（必须在项目根目录）：**

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\mvnw.cmd spring-boot:run
```

**窗口 B — 前端：**

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate\frontend
npm install
npm run dev
```

浏览器：**http://localhost:5173**

### 改完前端要更新 8080 页面

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\build-frontend.ps1
.\mvnw.cmd spring-boot:run
```

---

## 测试账号


| 角色    | 账号           | 密码       |
| ----- | ------------ | -------- |
| 学生    | 202301010101 | 123456   |
| 管理员   | admin        | admin123 |
| 超级管理员 | superadmin   | super123 |


---

## 验收清单（V1.1）

- 学生：登录 → 预约 → 二维码 → 管理员签到 → 暂离 → 返回 → 签退
- 学生：个人信息修改并保存
- 管理员：上传自习室分布图，学生端可见
- 管理员：审核用户、操作日志、统计导出

完整步骤见 [傻瓜式全局项目讲解 · 第 6 章](docs/01-使用指南/00-傻瓜式全局项目讲解.md#第-6-章功能验收打勾即完成必做)

---

## 文档导航


| 文档    | 路径                                                               |
| ----- | ---------------------------------------------------------------- |
| 启动流程（PowerShell） | [docs/01-使用指南/05-启动流程与PowerShell命令.md](docs/01-使用指南/05-启动流程与PowerShell命令.md) |
| 傻瓜式讲解 | [docs/01-使用指南/00-傻瓜式全局项目讲解.md](docs/01-使用指南/00-傻瓜式全局项目讲解.md)       |
| 架构说明  | [docs/02-架构说明/01-项目架构说明.md](docs/02-架构说明/01-项目架构说明.md) |
| 开发原则  | [docs/03-开发维护/00-开发原则.md](docs/03-开发维护/00-开发原则.md)       |
| 更新日志  | [docs/04-版本记录/03-更新日志.md](docs/04-版本记录/03-更新日志.md)           |
| 问题记录  | [docs/03-开发维护/问题记录.md](docs/03-开发维护/问题记录.md)             |


---

## 常见问题


| 问题         | 处理                              |
| ---------- | ------------------------------- |
| 不支持发行版本 21 | 安装 JDK 21，设置 `JAVA_HOME`        |
| 连不上数据库     | 确认 MySQL 运行、`DB_PASSWORD` 正确    |
| 页面没有新功能    | 使用 5173 dev 或重新 `npm run build` |
| 上传 404     | 重启后端，检查 `uploads/` 目录           |


更多 → [问题记录](docs/03-开发维护/问题记录.md)