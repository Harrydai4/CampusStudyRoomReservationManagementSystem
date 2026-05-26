# 06 — GitHub 傻瓜式提交全流程（组员 → 组长审核）

> **适用场景**：课程共同仓库（如 `CZF312/CampusStudyRoomReservationManagementSystem`），你是组员，要把 **CSRRMupdate V1.1** 交给组长审核，**不覆盖** 原版 `master`。  
> **你的账号示例**：`Harrydai4` · **分支示例**：`v1.1-harrydai4` · **PR 示例**：`#1`

---

## 零、先搞懂 4 个词（不用背 Git 命令）


| 词          | 白话          | 你要记住的                                        |
| ---------- | ----------- | -------------------------------------------- |
| **仓库**     | 网盘里的项目文件夹   | 共同仓库在 CZF312 名下；你有一份 **Fork 复印本**            |
| **分支**     | 作业本里的某一页    | 原版在 `master`；你的 V1.1 写在 `**v1.1-harrydai4`** |
| **Commit** | 保存一次修改      | 本地 `git commit` = 存档                         |
| **PR**     | 合并请求 / 审核链接 | 发给组长打开的网页，**不是 zip**                         |


**不覆盖原版** = 不直接改 `master`，只开 PR，等组长点 Merge。

---

## 一、流程总图

```text
【本地】CSRRMupdate 改好代码
    ↓ git init → add → commit
    ↓ 推送到【你的 Fork】分支 v1.1-harrydai4
    ↓ 合并共同仓库 master 历史（避免 PR 报 unrelated histories）
    ↓ 再 push
【GitHub】开 Pull Request → CZF312/master
    ↓ 微信把 PR 链接发给组长
【组长】Review → Merge pull request
    ↓
共同仓库 master 变为 V1.1
```

---

## 二、前置条件


| 项         | 要求              | 检查命令                                             |
| --------- | --------------- | ------------------------------------------------ |
| Git       | 已安装             | `git --version`                                  |
| GitHub 账号 | 已注册             | 能登录 github.com                                   |
| PAT       | 推送用令牌           | Settings → Developer settings → Tokens → 勾选 repo |
| 项目目录      | 仅 `CSRRMupdate` | 不要提交上层 `Database` 整个文件夹                          |


---

## 三、第 1 阶段：本地准备（只做一次）

### 3.1 安装 Git

下载：[https://git-scm.com/download/win](https://git-scm.com/download/win) → 安装后**重开 PowerShell** → `git --version`

### 3.2 配置身份（只做一次）

```powershell
git config --global user.name "你的GitHub用户名"
git config --global user.email "你的GitHub邮箱"
```

### 3.3 生成 PAT（推送密码）

1. GitHub 头像 → **Settings** → **Developer settings**
2. **Personal access tokens** → **Tokens (classic)** → **Generate new token**
3. 勾选 **repo** → 生成 → **复制保存**（只显示一次）
4. 以后 `git push` 时 **Password 填 PAT**，不是登录密码

### 3.4 本地构建自检

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\mvnw.cmd test
.\scripts\build-frontend.ps1
```

---

## 四、第 2 阶段：Fork 共同仓库（只做一次）

1. 打开：`https://github.com/CZF312/CampusStudyRoomReservationManagementSystem`
2. 右上角 **Fork** → Fork 到你账号
3. 得到：`https://github.com/Harrydai4/CampusStudyRoomReservationManagementSystem`
4. 若提示 “No available destinations” → **说明已经 Fork 过**，直接用你的 Fork 即可

---

## 五、第 3 阶段：本地 Git 初始化并提交

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate

git init
git branch -M main

# 确认密码文件被忽略（应有输出）
git check-ignore -v src\main\resources\application-local.properties

git add README.md pom.xml mvnw mvnw.cmd .gitignore .mvn .gitattributes
git add src frontend docs scripts uploads/README.md

git status
# 确认 staging 里没有 application-local.properties

git commit -m "feat: V1.1 完整版 — UI对齐、文档、前后端一体可运行"
```

---

## 六、第 4 阶段：推送到你的 Fork（独立分支）

```powershell
git remote add origin https://github.com/Harrydai4/CampusStudyRoomReservationManagementSystem.git
# 若已存在：git remote set-url origin https://github.com/Harrydai4/...

git checkout -b v1.1-harrydai4
git push -u origin v1.1-harrydai4
```

**不要** `git push origin main:master`，**不要** `--force`。

---

## 七、第 5 阶段：关联共同仓库历史（避免 PR 报错）

若 PR 页提示 **entirely different commit histories**，在本地执行：

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate

git remote add upstream https://github.com/CZF312/CampusStudyRoomReservationManagementSystem.git
# 若已存在可跳过

git fetch upstream
git fetch origin
git checkout v1.1-harrydai4

# 若提示 .gitattributes 未跟踪：
git add .gitattributes
git commit -m "chore: add .gitattributes"

git merge upstream/master --allow-unrelated-histories -m "chore: 关联共同仓库 master 历史"
```

**若大量冲突**（保留你的 V1.1）：

```powershell
git checkout --ours .
git add .
git commit -m "chore: 保留 V1.1 内容并关联 master 历史"

git push origin v1.1-harrydai4
```

---

## 八、第 6 阶段：创建 Pull Request

1. 打开（把用户名/仓库名换成你的）：
  `https://github.com/CZF312/CampusStudyRoomReservationManagementSystem/compare/master...Harrydai4:v1.1-harrydai4`
2. 应显示 **Able to merge** / **No conflicts**
3. 点 **Create pull request**
4. 标题示例：`feat: Harrydai4 提交 V1.1 完整实现`
5. 说明可附运行方式、测试账号（见 `05-启动流程与PowerShell命令.md`）

---

## 九、第 7 阶段：发给组长（组员必做）

复制发送：

```text
组长好，我已完成 V1.1，请审核合并：

PR 链接：
https://github.com/CZF312/CampusStudyRoomReservationManagementSystem/pull/1

完整代码分支（备用下载）：
https://github.com/Harrydai4/CampusStudyRoomReservationManagementSystem/tree/v1.1-harrydai4

说明：未直接改 master；PR 无冲突。clone 后配置 application-local.properties，mvnw spring-boot:run → 8080。
学生 202301010101/123456，管理员 admin/admin123。
```

**PR 链接** = 浏览器打开 PR 页面时地址栏的 URL。

---

## 十、组长会做什么（你理解即可）


| 组长操作                                   | 结果              |
| -------------------------------------- | --------------- |
| 打开 PR → Files changed                  | 看全部文件           |
| Merge pull request                     | master 更新为 V1.1 |
| 或 Code → Download ZIP / git clone 你的分支 | 下载完整项目试运行       |


**组员不要自己点 Merge**（除非组长授权）。

---

## 十一、常见现象 FAQ


| 现象                               | 是否正常 | 怎么办                        |
| -------------------------------- | ---- | -------------------------- |
| Fork 的 master 仍是 Initial commit  | ✅    | V1.1 在 `v1.1-harrydai4` 分支 |
| CZF312 master 仍是旧的               | ✅    | 等 PR Merge                 |
| PR 显示 unrelated histories        | 常见   | 做第七节 merge upstream        |
| push 要密码                         | ✅    | 填 PAT                      |
| merge 报 .gitattributes           | ✅    | 先 add 再 merge              |
| 本地有 application-local.properties | ✅    | 不要 git add，应在 .gitignore   |


---

## 十二、禁止提交清单


| 不要提交                           | 原因      |
| ------------------------------ | ------- |
| `application-local.properties` | 含数据库密码  |
| `target/`、`node_modules/`      | 可重新生成   |
| `uploads/`*（除 README）          | 运行时文件   |
| 兄弟目录 V1.0 原型包                  | 不是本交付目录 |


---

## 十三、提交后自检（模拟组长）

```powershell
cd D:\
git clone -b v1.1-harrydai4 https://github.com/Harrydai4/CampusStudyRoomReservationManagementSystem.git test-clone
cd test-clone
Copy-Item src\main\resources\application-local.properties.example src\main\resources\application-local.properties
# 编辑填密码
.\mvnw.cmd spring-boot:run
```

浏览器 [http://localhost:8080](http://localhost:8080) 能登录 = 合格。

---

## 十四、与本项目其他文档关系


| 文档                                                 | 用途          |
| -------------------------------------------------- | ----------- |
| [04-GitHub提交清单与说明.md](04-GitHub提交清单与说明.md)         | 文件清单、必交/禁交  |
| [05-启动流程与PowerShell命令.md](05-启动流程与PowerShell命令.md) | 本地运行        |
| [03-提交前全面自检清单.md](03-提交前全面自检清单.md)                 | 答辩前打勾       |
| [../03-开发维护/问题记录.md](../03-开发维护/问题记录.md)           | Bug 1～10 验收 |
| [../03-开发维护/最终验收对照.md](../03-开发维护/最终验收对照.md)       | 最终版总表       |


---

## 十五、你的进度打勾


| 步骤  | 内容                         | 完成  |
| --- | -------------------------- | --- |
| 1   | 安装 Git + PAT               | ☐   |
| 2   | Fork CZF312 仓库             | ☐   |
| 3   | 本地 test + build            | ☐   |
| 4   | git init / add / commit    | ☐   |
| 5   | push 到 `v1.1-harrydai4`    | ☐   |
| 6   | merge upstream（若 PR 报历史无关） | ☐   |
| 7   | 创建 PR                      | ☐   |
| 8   | 发链接给组长                     | ☐   |


全部打勾 = 组员侧 GitHub 流程完成。