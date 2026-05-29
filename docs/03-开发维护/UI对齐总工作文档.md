# UI 对齐总工作文档（原型 → 实现 → 验收）

> **版本**：V1.1 · **最后更新**：2026-05-25  
> **对照源（唯一 UI 真源）**：`CampusStudyRoomReservationManagementSystem-master/原型设计.html`（副本：`docs/07-原型资源/原型设计.html`）  
> **需求/交互规范**：`docs/05-需求与设计/概要设计与详细设计.md` §8–§9  
> **实现入口**：`frontend/src/App.vue` + `prototype-ui.css` + `styles.css`  
> **验收脚本**：`docs/01-使用指南/02-答辩验收清单.md`

---

## 1. 文档目的与使用方式

本文件是 **UI 对齐的唯一总台账**，比 `UI对齐进度管理.md` 更详尽，包含：

1. 每个模块的原型位置、设计文档条款、实现位置、验收步骤
2. 允许差异（答辩口述）与必须一致项（答辩演示）的区分
3. 全流程验收路径（学生 + 管理 + 会话）

**状态图例**：⬜ 未开始 · 🟡 部分完成 · ✅ 已对齐 · ⚠️ 文档允许差异

**更新规则**：每完成一项，同步更新本文件与 `UI对齐进度管理.md` 对应条目状态。

---

## 2. 对照关系（严谨性要求）


| 层级        | 文件                      | 作用                       |
| --------- | ----------------------- | ------------------------ |
| L1 视觉/布局  | `原型设计.html`             | 页面结构、组件 class、按钮文案、弹窗形态  |
| L2 交互/校验  | `概要设计与详细设计.md` §9       | 前端校验 + API + 状态机 + toast |
| L3 功能/API | 同上 §7 + 后端 `AppService` | 数据真实落地，禁止纯 mock          |
| L4 答辩     | `02-答辩验收清单.md`          | 老师现场操作路径                 |


**原则**：交互比像素优先；按钮必须有 **禁用态 / loading / toast / 跳转**；token 失效必须回登录页。

---

## 3. 阶段总览


| 阶段  | 模块      | 目标               | 状态  |
| --- | ------- | ---------------- | --- |
| P0  | M00–M01 | 壳层、登录、注册、会话      | ✅   |
| P1  | M02–M05 | 学生首页/预约/签到/我的    | ✅   |
| P2  | M06–M10 | 信用/统计/设置/通知/反馈   | ✅   |
| P3  | M11–M20 | 管理端全模块           | ✅   |
| P4  | M21     | 弹窗 modal-mask 统一 | ✅   |


---

## 4. 模块逐项台账

### M00 全局壳层


| ID      | 原型               | 要求          | 实现                                  | 状态  |
| ------- | ---------------- | ----------- | ----------------------------------- | --- |
| UI-0001 | `#device-frame`  | 手机紫边/桌面浅底   | `.device.mobile` + `.app-container` | ✅   |
| UI-0002 | `#app-container` | 圆角 overflow | `.app-container`                    | ✅   |
| UI-0003 | toast            | 2s 操作反馈     | `toast` + `notify()`                | ✅   |
| UI-0004 | fadeIn           | 切页动画        | `.page-fade` + `@keyframes fadeIn`  | ✅   |
| UI-0005 | bottom-nav       | 4 Tab + 下划线 | `.bottom-nav`                       | ✅   |


### M01 认证


| ID      | 原型/§9               | 要求        | 实现                                            | 状态  |
| ------- | ------------------- | --------- | --------------------------------------------- | --- |
| UI-0101 | `#page-login`       | 未登录必见登录页  | `v-if="!token"` + `clearSession`              | ✅   |
| UI-0102 | L541–543            | Logo+标题   | `.login-logo-box` `.login-title`              | ✅   |
| UI-0103 | L545–546            | 原生 input  | `.field` `.input`                             | ✅   |
| UI-0104 | L547                | 登录 API    | `loginStudent/Admin` + loading                | ✅   |
| UI-0105 | L548                | 忘记密码      | toast 提示                                      | ✅   |
| UI-0106 | `#registerModal`    | 注册弹窗      | `modal-mask` + 全字段                            | ✅   |
| UI-0107 | L552                | 切换管理员     | `loginRole`                                   | ✅   |
| UI-0108 | `#page-admin-login` | 管理员白卡片    | `.admin-login-page` `.admin-login-card`       | ✅   |
| UI-0109 | §9.2                | 确认密码+材料上传 | `registerPassword2` + `/auth/register/upload` | ✅   |
| UI-0110 | §9.1                | 401/失效回登录 | axios 拦截 + bootstrap                          | ✅   |
| UI-0111 | —                   | 退出        | `logout` → `clearSession`                     | ✅   |


### M02 首页


| ID      | 要求          | 实现                      | 状态  |
| ------- | ----------- | ----------------------- | --- |
| UI-0201 | header+通知角标 | topbar + badge          | ✅   |
| UI-0202 | hero 欢迎     | `.hero-card`            | ✅   |
| UI-0203 | 横向公告        | `.announce-row` + 置顶排序  | ✅   |
| UI-0204 | 今日预约 grid   | `.today-grid`           | ✅   |
| UI-0205 | 推荐自习室       | `.recommend-list` → 预约页 | ✅   |
| UI-0206 | 小贴士在推荐后     | `.hero-banner` 置底       | ✅   |


### M03 预约


| ID           | 要求         | 实现                          | 状态  |
| ------------ | ---------- | --------------------------- | --- |
| UI-0301      | 返回+标题      | `goBackStudent`             | ✅   |
| UI-0302      | 7 天 pill   | `dateOptions`               | ✅   |
| UI-0303      | 自习室选择      | select + 刷新座位               | ✅   |
| UI-0304      | 快捷时段 19–21 | `quickTimeSlots`            | ✅   |
| UI-0305–0307 | 时段/图例/筛选   | `.time-chip` `.filter-chip` | ✅   |
| UI-0308      | 分区网格       | `groupedSeats`              | ✅   |
| UI-0309      | 点座→确认弹窗    | `selectSeatDirect` + modal  | ✅   |
| UI-0310      | 成功→签到      | `createReservation`         | ✅   |


### M04 签到


| ID      | 要求       | 实现                                   | 状态  |
| ------- | -------- | ------------------------------------ | --- |
| UI-0401 | 状态 chip  | `.status`                            | ✅   |
| UI-0402 | 计时器      | `studySeconds` / 暂离倒计时               | ✅   |
| UI-0403 | 预约信息     | `.checkin-row`                       | ✅   |
| UI-0404 | QR 弹窗    | `#qrModal` → `qrModalOpen`           | ✅   |
| UI-0405 | 暂离/返回/签退 | API + 状态按钮                           | ✅   |
| UI-0406 | 签退确认+摘要  | `genericModal` + `checkoutModalOpen` | ✅   |
| UI-0407 | 反馈弹窗     | `feedbackModalOpen`                  | ✅   |


### M05–M10 我的子页


| 模块                | 要点                                 | 状态  |
| ----------------- | ---------------------------------- | --- |
| M05 profile       | `profile-head` + `profile-item` 菜单 | ✅   |
| M06 credit        | 圆环+守则+时间线                          | ✅   |
| M07 stats         | 日/周/月/年 + 柱图 + 预约/签到/违约摘要          | ✅   |
| M08 settings      | 仅通知+账号安全（资料在 profile 弹窗）           | ✅   |
| M09 notifications | 页头「全部已读」                           | ✅   |
| M10 feedback      | modal 表单+类型/严重程度                   | ✅   |


### M11–M20 管理端


| 模块      | 原型要点              | 实现                                    | 状态  |
| ------- | ----------------- | ------------------------------------- | --- |
| M11 仪表盘 | 四 KPI + 周趋势 + 实时表 | `admin-dashboard-grid` + API 扩展       | ✅   |
| M12 用户  | 信用列+分页+拒绝原因       | `pagedUsers` + `genericModal`         | ✅   |
| M13 自习室 | 内嵌表单+room-item    | `roomFormExpanded`                    | ✅   |
| M14 座位  | cell-grid + 属性编辑  | `seatEditOpen` + `updateSeat`         | ✅   |
| M15 预约  | 列表                | DataTable                             | ✅   |
| M16 签到  | 摄像头+token         | `scanCheckin`                         | ✅   |
| M17 公告  | 发布/编辑             | `saveAnnouncement`                    | ✅   |
| M18 统计  | 周期 Tab + 三视图      | `adminStatsPeriod` + usage/peak/share | ✅   |
| M19 反馈  | 标记处理              | `handleFeedback`                      | ✅   |
| M20 设置  | 管理员表+操作日志         | `/admin/admins` + logs                | ✅   |


### M21 弹窗统一


| 弹窗   | 原型 ID            | 实现                       | 状态  |
| ---- | ---------------- | ------------------------ | --- |
| 注册   | `#registerModal` | `modal-mask`             | ✅   |
| 确认预约 | genericModal     | `confirmReservationOpen` | ✅   |
| 二维码  | `#qrModal`       | `qrModalOpen`            | ✅   |
| 签退成功 | `#checkoutModal` | `checkoutModalOpen`      | ✅   |
| 通用确认 | `#genericModal`  | `genericModal`           | ✅   |
| 个人信息 | profile 弹窗       | `profileInfoOpen`        | ✅   |
| 反馈   | repair/feedback  | `feedbackModalOpen`      | ✅   |


---

## 5. 允许差异（答辩说明）


| 项                  | 说明                                                |
| ------------------ | ------------------------------------------------- |
| Element Plus 时间/下拉 | 文档允许 Vue3+EP，交互一致即可                               |
| 管理端反馈页             | 设计文档要求，原型 sidebar 无此项                             |
| 暂离/返回              | V1.1 功能，优于原型 mock                                 |
| 单元格子类编辑器           | 原型完整 seatMap 字符串编辑；实现用 seat 表属性 checkbox，**功能等价** |
| 修改密码               | V1.2 接口；入口保留并 toast 提示                            |


---

## 6. 全流程验收路径（一次性走通）

### 6.1 环境

```powershell
cd D:\SchoolWorkPlace\Database\CSRRMupdate
.\scripts\check-env.ps1
.\scripts\build-frontend.ps1
.\mvnw.cmd spring-boot:run
```

浏览器 **Ctrl+F5** 打开 [http://localhost:8080；若无登录页：`localStorage.clear()](http://localhost:8080；若无登录页：`localStorage.clear()); location.reload()`

### 6.2 学生端（202225220101 / 123456）

1. 登录 → 首页（公告、今日预约、推荐、小贴士）
2. 推荐自习室 → 预约页 → 快捷时段 → 选座 → 确认弹窗 → 成功 → 签到页
3. 出示二维码（弹窗）→ 管理员扫码后计时器走动
4. 暂离 → 返回 → 签退（确认 → 摘要弹窗）
5. 我的 → 预约/信用/统计/设置/反馈（弹窗）
6. 退出 → 回到登录页

### 6.3 管理端（admin / admin123）

1. 管理员登录（白卡片页）
2. 仪表盘四 KPI + 趋势图 + 实时预约表
3. 用户审核/禁用、自习室内嵌表单、座位网格编辑
4. 扫码签到、公告发布、统计三 Tab、反馈处理、设置日志

---

## 7. 后端 API 扩展（UI 支撑）


| API                                             | 用途                                                                       |
| ----------------------------------------------- | ------------------------------------------------------------------------ |
| `GET /admin/dashboard`                          | 增加 activeUsers、seatUsageRate、violationToday、weeklyTrend、liveReservations |
| `GET /statistics/my-study-duration?period=year` | 年报                                                                       |
| `POST /auth/register/upload`                    | 注册材料免登录上传                                                                |
| `GET /admin/admins`                             | 设置页管理员列表                                                                 |
| `PUT /admin/seats/{id}`                         | 座位属性编辑                                                                   |


---

## 8. 变更记录


| 日期         | 内容                                       |
| ---------- | ---------------------------------------- |
| 2026-05-25 | 初版：全模块台账 + P0–P4 完成标记                    |
| 2026-05-25 | 后端 dashboard 扩展；前端 modal 统一；管理端/学生端大批量对齐 |


---

## 9. 维护说明

- 新功能先更新 **§4 台账**，再改代码，最后跑 §6 验收路径  
- 像素级差异记录到 **§5**，避免重复劳动  
- 与 `UI对齐进度管理.md` 保持 ID 一致，答辩前两份文档状态应相同

