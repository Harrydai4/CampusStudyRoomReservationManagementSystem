<template>
  <div class="device" :class="{ desktop: isDesktop, mobile: !isDesktop }">
    <div class="app-container">
    <div v-if="toast" class="toast">{{ toast }}</div>

    <section v-if="!token" :class="loginRole === 'admin' ? 'admin-login-page' : 'login-page'">
      <template v-if="loginRole === 'student'">
      <div class="login-logo-box">🎓</div>
      <div class="login-title">校园自习室预约系统</div>
      <div class="login-subtitle">{{ loginRole === 'student' ? 'Campus Study Room Reservation' : 'Study Room Admin Console' }}</div>
      <div class="login-card">
        <div class="field">
          <label>学号</label>
          <input v-model="studentLogin.username" class="input" placeholder="请输入学号" autocomplete="username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="studentLogin.password" class="input" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button type="button" class="btn btn-primary btn-block" :disabled="authLoading" @click="loginStudent">{{ authLoading ? '登录中…' : '登录' }}</button>
        <div class="login-links">
          <button type="button" @click="forgetPassword">忘记密码？</button>
          <button type="button" @click="openRegister">注册账号 →</button>
        </div>
        <button type="button" class="btn btn-outline btn-block" @click="loginRole = 'admin'">🔧 切换管理员登录</button>
      </div>
      </template>
      <div v-else class="admin-login-card">
        <h2 class="modal-title">管理员登录</h2>
        <div class="field">
          <label>管理员账号</label>
          <input v-model="adminLogin.account" class="input" placeholder="请输入管理员账号" autocomplete="username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="adminLogin.password" class="input" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <button type="button" class="btn btn-primary btn-block" :disabled="authLoading" @click="loginAdmin">{{ authLoading ? '登录中…' : '登录' }}</button>
        <button type="button" class="btn btn-outline btn-block" @click="loginRole = 'student'">🎓 切换学生登录</button>
      </div>
    </section>

    <section v-else-if="role === 'STUDENT'" class="student-app">
      <header class="topbar">
        <button class="icon-btn" v-if="studentPage !== 'home'" @click="goBackStudent">←</button>
        <h1>{{ studentTitle }}</h1>
        <button v-if="studentPage === 'notifications'" type="button" class="header-action" @click="readAllNotifications">全部已读</button>
        <button class="icon-btn bell-btn" v-else-if="studentPage === 'home'" @click="openNotifications">🔔<span v-if="unreadCount" class="badge">{{ unreadCount }}</span></button>
        <span v-else class="topbar-spacer"></span>
      </header>

      <main class="content page-fade">
        <template v-if="studentPage === 'home'">
          <div class="hero-card">
            <strong class="hero-title">你好，{{ me.name || '同学' }}</strong>
            <span class="hero-sub">{{ homeDateText }}</span>
          </div>
          <h2 class="section-title">📣 公告通知</h2>
          <div class="announce-row">
            <article class="announce-card" v-for="a in sortedAnnouncements.slice(0, 4)" :key="a.id" @click="readAnnouncement(a)">
              <div class="announce-tag">📌 {{ a.pinned ? '系统通知' : (a.type || '公告') }}</div>
              <strong>{{ a.title }}</strong>
              <p>{{ formatDate(a.published_at || a.created_at) }}</p>
            </article>
          </div>
          <h2 class="section-title">📅 今日预约</h2>
          <article class="card" v-if="todayReservation">
            <div class="today-head">
              <strong>📌 今日预约</strong>
              <span class="status" :class="todayReservation.status">{{ statusText(todayReservation.status) }}</span>
            </div>
            <div class="today-grid">
              <div class="label">自习室</div><div class="value">{{ todayReservation.roomName }}</div>
              <div class="label">座位</div><div class="value">{{ todayReservation.seatNo }}</div>
              <div class="label">时段</div><div class="value">{{ timeRangeText(todayReservation) }}</div>
            </div>
          </article>
          <div v-else class="card empty muted">今日暂无预约，可前往预约页选择座位。</div>
          <h2 class="section-title">🏠 推荐自习室</h2>
          <div class="recommend-list">
            <article class="recommend-item" v-for="r in rooms" :key="r.id" @click="selectRoom(r.id)">
              <div class="recommend-icon">🏫</div>
              <div>
                <strong>{{ r.name }}</strong>
                <p class="muted">{{ r.location }} · 普通</p>
              </div>
              <div class="recommend-badge">余{{ r.availableSeats }}座</div>
            </article>
          </div>
          <div class="hero-banner">
            <h4>💡 学习小贴士</h4>
            <p>番茄工作法：每学习 25 分钟休息 5 分钟，建议预约 2-3 小时提高效率。</p>
          </div>
        </template>

        <template v-if="studentPage === 'reservation'">
          <div v-if="creditBlocked" class="card warn-hint" style="border-color:#fecaca;background:#fff1f2;color:#b91c1c;font-weight:700">
            信用积分不足，暂不可预约。请前往「我的 → 信用积分」查看详情。
          </div>
          <h2>🗓️ 选择日期</h2>
          <div class="date-rail">
            <button v-for="d in dateOptions" :key="d.date" class="date-pill" :class="{ active: reservationForm.date === d.date }" @click="setReservationDate(d.date)">
              <span>{{ d.label }}</span>
              <strong>{{ d.day }}</strong>
              <small>{{ d.month }}月</small>
            </button>
          </div>

          <h2 class="section-title">⏱️ 选择时段</h2>
          <p v-if="currentRoom" class="scanner-hint">当前自习室开放时间：{{ currentRoomOpenTime }} — {{ currentRoomCloseTime }}（以下选项随所选自习室自动变化）</p>
          <div class="time-slots">
            <button v-for="slot in availableQuickTimeSlots" :key="slot.label" type="button" class="time-chip" :class="{ active: isQuickSlotActive(slot), disabled: slot.expired }" :disabled="slot.expired" @click="applyQuickSlot(slot)">{{ slot.label }}{{ slot.expired ? '（已过期）' : '' }}</button>
          </div>
          <div class="card reserve-config">
            <div class="time-select-row">
              <label>开始</label>
              <el-select v-model="reservationForm.startTime" placeholder="请选择开始时间" :teleported="false" @change="handleStartTimeChange">
                <el-option v-for="t in startTimeOptions" :key="`s-${t}`" :label="t" :value="t" />
              </el-select>
              <span>→</span>
              <label>结束</label>
              <el-select v-model="reservationForm.endTime" placeholder="请选择结束时间" :teleported="false" @change="handleEndTimeChange">
                <el-option v-for="t in endTimeOptions" :key="`e-${t}`" :label="t" :value="t" />
              </el-select>
            </div>
          </div>

          <h2 class="section-title">🧮 预约配置</h2>
          <div class="card reserve-config">
            <label>自习室</label>
            <el-select v-model="reservationForm.roomId" @change="handleRoomChange">
              <el-option v-for="r in rooms" :key="r.id" :label="`${r.name}（余${r.availableSeats}）`" :value="r.id" />
            </el-select>
          </div>
          <div class="seat-filters">
            <button v-for="f in seatFilterOptions" :key="f.key" type="button" class="filter-chip" :class="{ active: seatFilter === f.key }" @click="seatFilter = f.key">{{ f.label }}</button>
          </div>
          <div class="legend legend-row">
            <span><i class="legend-dot free"></i>可选</span><span><i class="legend-dot busy"></i>不可用</span><span><i class="legend-dot sel"></i>已选</span>
          </div>

          <div class="seat-overview">
            <div class="seat-overview-head"><strong>🗺️ 座位分布图</strong><span class="muted">{{ currentRoom?.name || '自习室' }}</span></div>
            <img v-if="roomLayoutImage" class="seat-layout-image" :src="roomLayoutImage" :alt="`${currentRoom?.name || '自习室'}座位分布图`" />
            <div v-else class="seat-layout-empty">管理员尚未上传该自习室的座位分布图</div>
          </div>

          <div class="seat-sections">
            <section v-for="sec in groupedSeats" :key="sec.name" class="seat-section">
              <div class="seat-section-title">{{ sec.name }}（可预约 {{ sec.availableCount }}）</div>
              <div class="seat-section-grid">
                <button v-for="s in sec.seats" :key="s.id" type="button" class="seat" :class="seatVisualClass(s)" :disabled="!canSelectSeat(s)" @click="openSeatDetail(s)">{{ s.seat_no }}</button>
              </div>
            </section>
          </div>
          <button class="primary-action reserve-submit" :disabled="!selectedSeat || creditBlocked" @click="openConfirmReservation">确认预约 {{ selectedSeat ? selectedSeat.seat_no : '' }}</button>
        </template>

        <template v-if="studentPage === 'checkin'">
          <div class="card check-card check-hero">
            <span class="status" :class="activeReservation?.status || 'PENDING'">{{ activeReservation ? statusText(activeReservation.status) : '暂无预约' }}</span>
            <div class="timer">{{ timerText }}</div>
            <div v-if="activeReservation?.status === 'TEMP_LEAVE'" class="away-hint">暂离倒计时：{{ timerText }}</div>
          </div>
          <div class="card reservation-detail-card checkin-info-card" v-if="activeReservation">
            <div class="checkin-row"><div class="k">自习室</div><div class="v">{{ activeReservation.roomName }}</div></div>
            <div class="checkin-row"><div class="k">座位号</div><div class="v">{{ activeReservation.seatNo }}</div></div>
            <div class="checkin-row"><div class="k">预约时段</div><div class="v">{{ timeRangeText(activeReservation) }}</div></div>
            <div class="checkin-row"><div class="k">预约日期</div><div class="v">{{ formatDate(activeReservation.reserve_date) }}</div></div>
          </div>
          <div class="card check-actions" v-if="activeReservation">
            <template v-if="activeReservation.status === 'PENDING'">
              <div class="check-wait-card card">
                <strong>等待管理员签到</strong>
                <p class="check-student-no">学号：<span>{{ studentNoDisplay }}</span></p>
                <div v-if="checkinQrSvg" class="qr-image checkin-qr" v-html="checkinQrSvg"></div>
                <p class="muted check-wait-tip">可向管理员<strong>报学号</strong>，或出示上方二维码供管理员<strong>拍照扫码</strong>（无需 token）</p>
              </div>
              <div class="check-actions-state">
                <button type="button" class="round-action warning-round" @click="openFeedbackModal">
                  <span>💬</span><strong>问题反馈</strong>
                </button>
              </div>
              <div class="check-hint">{{ checkinWindowHint }}</div>
            </template>
            <template v-else-if="activeReservation.status === 'USING'">
              <div class="check-actions-state">
                <button type="button" class="round-action danger-round" @click="confirmCheckout">
                  <span>🚪</span><strong>签退</strong>
                </button>
                <button type="button" class="round-action" @click="startTempLeave">
                  <span>🚶</span><strong>暂离</strong>
                </button>
                <button type="button" class="round-action warning-round" @click="openFeedbackModal">
                  <span>💬</span><strong>问题反馈</strong>
                </button>
              </div>
              <div class="check-hint">请在预约结束前完成签退。</div>
            </template>
            <template v-else-if="activeReservation.status === 'TEMP_LEAVE'">
              <div class="check-actions-state">
                <button type="button" class="round-action primary-round" @click="endTempLeave">
                  <span>↩️</span><strong>返回座位</strong>
                </button>
                <button type="button" class="round-action warning-round" @click="openFeedbackModal">
                  <span>💬</span><strong>问题反馈</strong>
                </button>
              </div>
              <div class="away-hint">{{ tempLeaveHint }}</div>
            </template>
          </div>
          <div v-else class="card empty muted">当前没有进行中的预约，可前往预约页选座。</div>
        </template>

        <template v-if="studentPage === 'profile'">
          <div class="profile-head">
            <div class="avatar">{{ (me.name || '同').slice(0, 1) }}</div>
            <div>
              <strong>{{ me.name || '同学' }}</strong>
              <p class="muted">学号：{{ me.student_no || me.username }}</p>
              <p class="muted">{{ me.college || '计算机学院' }}</p>
            </div>
          </div>
          <div class="profile-group-title">功能服务</div>
          <div class="profile-menu">
            <button type="button" class="profile-item" @click="studentPage = 'myres'"><span>📋</span><span>我的预约</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item" @click="studentPage = 'credit'"><span>⭐</span><span>信用积分</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item" @click="studentPage = 'stats'; drawStudentChart()"><span>📊</span><span>学习统计</span><span class="arrow">›</span></button>
          </div>
          <div class="profile-group-title">账号与安全</div>
          <div class="profile-menu">
            <button type="button" class="profile-item" @click="openProfileInfo"><span>📝</span><span>个人信息</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item" @click="openChangePassword"><span>🔐</span><span>修改密码</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item" @click="studentPage = 'settings'"><span>⚙️</span><span>设置</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item" @click="aboutOpen = true"><span>ℹ️</span><span>关于系统</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item" @click="openFeedbackModal"><span>💬</span><span>问题反馈</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item danger" @click="logout"><span>🚪</span><span>退出登录</span><span class="arrow">›</span></button>
          </div>
        </template>

        <template v-if="studentPage === 'myres'">
          <div class="filter-row">
            <button v-for="s in reservationTabs" :key="s.key" :class="{ active: reservationStatus === s.key }" @click="reservationStatus = s.key">{{ s.label }}</button>
          </div>
          <ReservationCard v-for="r in shownReservations" :key="r.id" :item="r" :status-text="statusText" @cancel="cancelReservation(r)" />
        </template>

        <template v-if="studentPage === 'credit'">
          <div class="card credit-ring-card">
            <div class="credit-ring" :style="{ '--score': creditPercent + '%' }">
              <strong>{{ credit.score }}</strong>
              <span>/ {{ CREDIT_SCORE_MAX }}</span>
            </div>
            <b>{{ creditLevel }}</b>
          </div>
          <div class="credit-metrics">
            <div class="card"><strong>{{ reservations.length }}</strong><span>总预约次数</span></div>
            <div class="card"><strong>{{ checkinCount }}</strong><span>准时签到</span></div>
            <div class="card"><strong>{{ violationCount }}</strong><span>违约次数</span></div>
          </div>
          <h2 class="section-title">📜 积分变动记录</h2>
          <div class="credit-rules card">
            <div class="credit-rule"><strong>按时签到</strong> +5 分；主动取消预约 -50 分；超时未签到 -50 分；暂离超时 -30 分。</div>
            <div class="credit-rule"><strong>积分上限</strong> {{ CREDIT_SCORE_MAX }} 分；280 分以上优秀，200 分以上良好。</div>
          </div>
          <div class="timeline">
            <div v-for="l in credit.logs" :key="l.id" class="timeline-item">
              <strong :class="Number(l.change_value) >= 0 ? 'credit-gain' : 'credit-deduct'">{{ Number(l.change_value) > 0 ? '+' : '' }}{{ l.change_value }}</strong>
              <span>{{ l.reason }}</span>
              <small>{{ l.created_at }}</small>
            </div>
          </div>
        </template>

        <template v-if="studentPage === 'stats'">
          <div class="stats-tabs">
            <button v-for="p in statPeriods" :key="p.key" :class="{ active: statPeriod === p.key }" @click="changeStatPeriod(p.key)">{{ p.label }}</button>
          </div>
          <div class="stat-summary-grid">
            <div class="card"><strong>{{ totalStudyHours }}</strong><span>小时</span><b>总学习时长</b></div>
            <div class="card"><strong>{{ averageStudyHours }}</strong><span>小时</span><b>日均时长</b></div>
            <div class="card"><strong>{{ studyDays }}</strong><span>天</span><b>学习天数</b></div>
          </div>
          <div class="card stat-card">
            <h2>{{ studyChartTitle }}</h2>
            <div class="bar-chart-lite">
              <div v-for="b in studyBars" :key="b.label" class="bar-col">
                <strong>{{ b.value }}<span class="bar-unit">小时</span></strong>
                <div class="bar-track">
                  <span :style="{ height: `${barHeight(b.value)}%` }"></span>
                </div>
                <small>{{ b.label }}</small>
              </div>
            </div>
          </div>
          <div class="card study-advice">
            <strong>📈 学习建议</strong>
            <p>{{ studyAdvice }}</p>
          </div>
        </template>

        <template v-if="studentPage === 'feedback'">
          <FeedbackBox @submit="submitFeedback" />
        </template>

        <template v-if="studentPage === 'notifications'">
          <el-button plain @click="readAllNotifications">全部已读</el-button>
          <article class="notif-item" :class="{ read: n.read_flag }" v-for="n in notifications" :key="n.id" @click="readNotification(n)">
            <div class="notif-icon">🔔</div>
            <div>
              <strong><span v-if="!n.read_flag" class="dot"></span>{{ n.title }}</strong>
              <p class="muted">{{ n.content }}</p>
            </div>
          </article>
        </template>

        <template v-if="studentPage === 'settings'">
          <div class="profile-group-title">账号与安全</div>
          <div class="profile-menu">
            <button type="button" class="profile-item" @click="openChangePassword"><span>🔐</span><span>修改密码</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item"><span>📱</span><span>手机绑定</span><span class="muted">{{ me.phone || '未绑定' }}</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item"><span>✉️</span><span>邮箱绑定</span><span class="muted">{{ me.email || '未绑定' }}</span><span class="arrow">›</span></button>
          </div>
          <div class="profile-group-title">通知偏好</div>
          <div class="card">
            <div class="setting-item"><span>预约提醒</span><button type="button" class="switch" :class="{ on: notifyPrefs.reservation }" @click="toggleNotifyPref('reservation')"></button></div>
            <div class="setting-item"><span>签到提醒</span><button type="button" class="switch" :class="{ on: notifyPrefs.checkin }" @click="toggleNotifyPref('checkin')"></button></div>
            <div class="setting-item"><span>公告通知</span><button type="button" class="switch" :class="{ on: notifyPrefs.announcement }" @click="toggleNotifyPref('announcement')"></button></div>
            <div class="setting-item"><span>免打扰模式</span><button type="button" class="switch" :class="{ on: notifyPrefs.dnd }" @click="toggleNotifyPref('dnd')"></button></div>
          </div>
          <div class="profile-group-title">通用</div>
          <div class="profile-menu">
            <button type="button" class="profile-item" @click="openFeedbackModal"><span>❓</span><span>帮助与反馈</span><span class="arrow">›</span></button>
            <button type="button" class="profile-item" @click="aboutOpen = true"><span>ℹ️</span><span>关于系统</span><span class="muted">v1.1</span><span class="arrow">›</span></button>
          </div>
        </template>
      </main>

      <nav class="bottom-nav">
        <button v-for="n in studentNav" :key="n.page" :class="{ active: activeStudentTab === n.page }" @click="studentPage = n.page">
          <span>{{ n.icon }}</span>
          <b>{{ n.label }}</b>
        </button>
      </nav>
    </section>

    <section v-else class="admin-app admin-shell">
      <aside class="admin-sidebar" v-if="isDesktop">
        <strong>管理后台</strong>
        <button v-for="n in adminNav" :key="n.page" type="button" class="admin-side-item" :class="{ active: adminPage === n.page }" @click="openAdmin(n.page)">{{ n.icon }} {{ n.label }}</button>
        <button type="button" class="admin-side-item" @click="logout">🚪 退出</button>
        <div class="admin-profile-chip" aria-label="当前管理员信息">
          <div class="admin-profile-avatar">{{ adminProfileInitial }}</div>
          <div class="admin-profile-meta">
            <strong>{{ me.name || me.account || '管理员' }}</strong>
            <span>{{ adminRoleLabel }}</span>
          </div>
        </div>
      </aside>
      <div class="admin-main">
        <header class="topbar">
          <h1>{{ adminNav.find(n => n.page === adminPage)?.label }}</h1>
          <button class="icon-btn" @click="logout">退出</button>
        </header>
        <nav class="admin-tabs" v-if="!isDesktop">
          <button v-for="n in adminNav" :key="n.page" :class="{ active: adminPage === n.page }" @click="openAdmin(n.page)">{{ n.label }}</button>
        </nav>

        <main class="content admin-content">
          <template v-if="adminPage === 'users'">
            <div class="admin-head-actions">
              <h3 class="section-title">学生用户管理</h3>
              <button type="button" class="btn btn-primary" @click="exportUsersCsv">导出 CSV</button>
            </div>
            <p class="scanner-hint">审核注册申请、禁用/启用学生账号；导出包含当前筛选条件下的全部学生。</p>
            <el-input v-model="userKeyword" placeholder="搜索学号或姓名" @input="loadUsers" />
            <div class="filter-row user-audit-filters">
              <button v-for="f in userAuditFilters" :key="f.key" type="button" :class="{ active: userAuditFilter === f.key }" @click="userAuditFilter = f.key; loadUsers()">{{ f.label }}</button>
            </div>
            <DataTable :rows="pagedUsers" :columns="['student_no','name','college','credit_score','auditLabel','statusLabel']" empty-text="暂无用户数据">
              <template #actions="{ row }">
                <el-button size="small" @click="openUserDetail(row)">详情</el-button>
                <el-button v-if="row.audit_status === 'PENDING'" size="small" type="success" @click="approve(row)">通过</el-button>
                <el-button v-if="row.audit_status === 'PENDING'" size="small" type="warning" @click="reject(row)">拒绝</el-button>
                <el-button v-if="row.accountStatus !== 'DISABLED' && row.audit_status === 'APPROVED'" size="small" @click="disable(row)">禁用</el-button>
                <el-button v-if="row.accountStatus === 'DISABLED'" size="small" @click="enable(row)">启用</el-button>
              </template>
            </DataTable>
            <div class="admin-pager">
              <button v-for="p in userTotalPages" :key="p" type="button" :class="{ active: userPage === p }" @click="userPage = p">{{ p }}</button>
            </div>
            <p v-if="users.length" class="admin-pager-meta scanner-hint">共 {{ users.length }} 条 · 第 {{ userPage }}/{{ userTotalPages }} 页</p>
          </template>

          <template v-if="adminPage === 'admins'">
            <div class="admin-head-actions">
              <h3>管理员管理</h3>
              <button v-if="isSuperAdmin" type="button" class="btn btn-primary" @click="openAdminForm()">新增管理员</button>
            </div>
            <p v-if="!isSuperAdmin" class="scanner-hint">仅超级管理员可新增、编辑或禁用其他管理员；您当前只能查看自己的账号信息。</p>
            <p v-else class="scanner-hint">超级管理员可分配图书馆负责人、新增/编辑/禁用普通管理员账号。</p>
            <el-input v-model="adminKeyword" placeholder="搜索账号或姓名" clearable />
            <div class="filter-row user-audit-filters">
              <button v-for="f in adminStatusFilters" :key="f.key" type="button" :class="{ active: adminStatusFilter === f.key }" @click="adminStatusFilter = f.key">{{ f.label }}</button>
            </div>
            <DataTable :rows="pagedAdminAccounts" :columns="adminAccountColumns" empty-text="暂无管理员">
              <template #actions="{ row }">
                <template v-if="isSuperAdmin">
                  <el-button v-if="row.role !== 'SUPER_ADMIN'" size="small" @click="openAdminForm(row)">编辑</el-button>
                  <el-button v-if="row.status !== 'DISABLED' && row.id !== me.id && row.role !== 'SUPER_ADMIN'" size="small" type="warning" @click="disableAdminAccount(row)">禁用</el-button>
                  <el-button v-if="row.status === 'DISABLED'" size="small" type="success" @click="enableAdminAccount(row)">启用</el-button>
                </template>
                <span v-else class="muted">—</span>
              </template>
            </DataTable>
            <AdminPager v-model:page="adminAccountPage" :total="adminAccountTotalPages" :count="filteredAdminAccounts.length" />
          </template>

          <template v-if="adminPage === 'rooms'">
            <div class="admin-head-actions">
              <h3>自习室管理</h3>
              <button v-if="isSuperAdmin" type="button" class="btn btn-primary" @click="openRoomFormCreate">新增自习室</button>
            </div>
            <p v-if="!isSuperAdmin" class="scanner-hint">普通管理员仅可编辑本人负责的自习室；点击「编辑」可在同一界面管理座位网格。</p>
            <p v-else class="scanner-hint">超级管理员可新增/删除自习室，并为每个自习室指定图书馆负责人。点击「编辑」可在同一界面管理座位网格。</p>
            <el-input v-model="roomKeyword" placeholder="搜索名称、位置或楼层" clearable />
            <div class="filter-row user-audit-filters">
              <button v-for="f in roomStatusFilters" :key="f.key" type="button" :class="{ active: roomStatusFilter === f.key }" @click="roomStatusFilter = f.key">{{ f.label }}</button>
            </div>
            <article class="room-item" v-for="r in pagedRooms" :key="r.id">
              <div>
                <div class="room-item-head"><strong>{{ r.name }}</strong><span class="mini-badge active">余 {{ r.availableSeats ?? r.available_seats ?? 0 }}</span></div>
                <p class="muted">{{ r.location }} · {{ r.floor || '未设置' }} · {{ roomStatusText(r.status) }}</p>
                <div class="room-tags">
                  <span v-for="tag in parseRoomFacilities(r)" :key="tag" class="room-tag">{{ tag }}</span>
                </div>
              </div>
              <div>
                <button type="button" class="btn btn-outline" @click="editRoom(r)">编辑</button>
                <button v-if="isSuperAdmin" type="button" class="btn btn-danger" @click="deleteRoom(r)">删除</button>
              </div>
            </article>
            <AdminPager v-model:page="roomPage" :total="roomTotalPages" :count="filteredRooms.length" />
          </template>

          <template v-if="adminPage === 'reservations'">
            <p class="scanner-hint">可按学号、姓名、预约号、自习室筛选；违约记录可在此撤销并恢复信用分。</p>
            <el-input v-model="reservationKeyword" placeholder="搜索学号、姓名、预约号或自习室" clearable />
            <div class="filter-row user-audit-filters">
              <button v-for="f in reservationAdminStatusFilters" :key="f.key" type="button" :class="{ active: reservationStatusFilter === f.key }" @click="reservationStatusFilter = f.key">{{ f.label }}</button>
            </div>
            <el-select v-model="reservationRoomFilter" placeholder="全部自习室" clearable style="min-width:220px;margin-bottom:12px">
              <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
            </el-select>
            <DataTable :rows="pagedAdminReservations" :columns="['reservation_no','studentName','roomName','seatNo','reserve_date','status','cancel_reason']" empty-text="暂无预约记录">
              <template #actions="{ row }">
                <el-button v-if="['VIOLATED','AUTO_CANCELLED'].includes(row._rawStatus)" size="small" type="warning" @click="openRevokeViolation(row)">撤销违约</el-button>
                <span v-else class="muted">—</span>
              </template>
            </DataTable>
            <AdminPager v-model:page="reservationPage" :total="reservationTotalPages" :count="filteredAdminReservations.length" />
          </template>

          <template v-if="adminPage === 'checkins'">
            <div class="card scan-box">
              <p class="scanner-hint">{{ scanHint || '优先「确认签到」输入学号（最稳）；拍照扫码为辅助，部分手机因照片格式/屏幕摩尔纹可能识别失败。' }}</p>
              <div class="scanner-toolbar">
                <button type="button" class="btn btn-primary" :disabled="scanBusy" @click="triggerPhotoScan">{{ scanBusy ? '处理中…' : '拍照扫码' }}</button>
                <button type="button" class="btn btn-outline" :disabled="scanBusy || !scanStudentNo.trim()" @click="scanCheckin">{{ scanBusy ? '提交中…' : '确认签到' }}</button>
                <input ref="scanPhotoInput" type="file" accept="image/*" capture="environment" class="scan-photo-input" @change="onScanPhotoSelected" />
              </div>
              <div class="scan-student-row">
                <input v-model="scanStudentNo" class="input" placeholder="请输入学生学号，如 202301010101" maxlength="20" :disabled="scanBusy" @keyup.enter="scanCheckin" />
              </div>
            </div>
            <el-input v-model="checkinKeyword" placeholder="搜索学号、姓名、自习室或座位" clearable />
            <div class="filter-row user-audit-filters">
              <button v-for="f in checkinResultFilters" :key="f.key" type="button" :class="{ active: checkinResultFilter === f.key }" @click="checkinResultFilter = f.key">{{ f.label }}</button>
            </div>
            <DataTable :rows="pagedCheckins" :columns="['studentName','roomName','seatNo','checkin_time','checkout_time','result']" empty-text="暂无签到记录" />
            <AdminPager v-model:page="checkinPage" :total="checkinTotalPages" :count="filteredCheckins.length" />
            <h3 class="section-title">实时预约</h3>
            <p class="scanner-hint">待签到、使用中、暂离中的预约（进入本页时自动刷新）。</p>
            <DataTable :rows="pagedLiveReservations" :columns="['studentNo','studentName','roomName','seatNo','reserveDate','status']" empty-text="暂无进行中的预约" />
            <AdminPager v-model:page="liveReservationPage" :total="liveReservationTotalPages" :count="decoratedLiveReservations.length" />
          </template>

          <template v-if="adminPage === 'announcements'">
            <el-input v-model="announcementKeyword" placeholder="搜索公告标题或内容" clearable style="margin-bottom:12px" />
            <el-button type="primary" @click="editAnnouncement()">发布公告</el-button>
            <article class="card announcement" v-for="a in pagedAnnouncements" :key="a.id">
              <strong>{{ a.title }}</strong><p>{{ a.content }}</p>
              <el-button size="small" @click="editAnnouncement(a)">编辑</el-button>
            </article>
            <AdminPager v-model:page="announcementPage" :total="announcementTotalPages" :count="filteredAnnouncements.length" />
          </template>

          <template v-if="adminPage === 'statistics'">
            <div class="admin-head-actions">
              <h3>统计分析</h3>
              <button type="button" class="btn btn-primary" @click="downloadReport">导出报表</button>
            </div>
            <el-select v-model="adminStatsRoomId" placeholder="全部自习室（汇总）" clearable style="width:100%;max-width:360px;margin-bottom:12px" @change="loadAdminStatistics">
              <el-option label="全部自习室（汇总）" :value="null" />
              <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
            </el-select>
            <div class="period-tabs adminStatsRange">
              <button type="button" :class="{ active: adminStatsRangeMode === 'current' }" @click="changeAdminStatsRangeMode('current')">当期</button>
              <button type="button" :class="{ active: adminStatsRangeMode === 'past' }" @click="changeAdminStatsRangeMode('past')">往期</button>
            </div>
            <div class="period-tabs adminStatsPeriod">
              <button type="button" :class="{ active: adminStatsPeriod === 'day' }" @click="changeAdminStatsPeriod('day')">日报</button>
              <button type="button" :class="{ active: adminStatsPeriod === 'week' }" @click="changeAdminStatsPeriod('week')">周报</button>
              <button type="button" :class="{ active: adminStatsPeriod === 'month' }" @click="changeAdminStatsPeriod('month')">月报</button>
              <button type="button" :class="{ active: adminStatsPeriod === 'year' }" @click="changeAdminStatsPeriod('year')">年报</button>
            </div>
            <div class="stat-view-tabs">
              <button type="button" :class="{ active: statAdminView === 'usage' }" @click="switchStatAdminView('usage')">使用统计</button>
              <button type="button" :class="{ active: statAdminView === 'peak' }" @click="switchStatAdminView('peak')">高峰分析</button>
              <button type="button" :class="{ active: statAdminView === 'share' }" @click="switchStatAdminView('share')">自习室占比</button>
            </div>
            <p class="scanner-hint">当前统计：{{ adminStatsScopeLabel }} · {{ adminStatsReport.summary?.periodLabel || '今日' }} · {{ adminStatsReport.summary?.rangeWindowLabel || '' }}</p>
            <div class="admin-dashboard-grid">
              <div class="stat-card"><div class="lbl">总预约</div><div class="num">{{ adminStatSummary.totalReserve }}<span class="stat-unit">次</span></div></div>
              <div class="stat-card"><div class="lbl">使用中</div><div class="num">{{ adminStatSummary.usingCount }}<span class="stat-unit">人</span></div></div>
              <div class="stat-card"><div class="lbl">签到率</div><div class="num">{{ adminStatSummary.checkinRate }}<span class="stat-unit">%</span></div></div>
              <div class="stat-card"><div class="lbl">平均信用分</div><div class="num">{{ adminStatSummary.avgCredit }}<span class="stat-unit">分</span></div></div>
            </div>
            <div class="card"><div ref="usageChart" class="chart"></div></div>
          </template>

          <template v-if="adminPage === 'feedback'">
            <el-input v-model="feedbackKeyword" placeholder="搜索学号、姓名、类型或反馈内容" clearable />
            <div class="filter-row user-audit-filters">
              <button v-for="f in feedbackStatusFilters" :key="f.key" type="button" :class="{ active: feedbackStatusFilter === f.key }" @click="feedbackStatusFilter = f.key">{{ f.label }}</button>
            </div>
            <DataTable :rows="pagedAdminFeedback" :columns="['studentName','roomName','seatNo','type','severity','content','status']" empty-text="暂无反馈">
              <template #actions="{ row }">
                <el-button v-if="row._rawStatus === 'PENDING' || row._rawStatus === 'PROCESSING'" size="small" type="primary" @click="openFeedbackHandle(row)">标记处理</el-button>
                <span v-else class="muted">已处理</span>
              </template>
            </DataTable>
            <AdminPager v-model:page="feedbackPage" :total="feedbackTotalPages" :count="filteredAdminFeedback.length" />
          </template>

          <template v-if="adminPage === 'settings'">
            <div class="card">当前管理员：{{ me.name }} · {{ me.role }}</div>
            <h3>最近操作日志</h3>
            <el-input v-model="logKeyword" placeholder="搜索模块、操作或详情" clearable />
            <div class="filter-row user-audit-filters">
              <button v-for="f in logModuleFilters" :key="f.key" type="button" :class="{ active: logModuleFilter === f.key }" @click="logModuleFilter = f.key">{{ f.label }}</button>
            </div>
            <DataTable :rows="pagedOperationLogs" :columns="['module','action','target_type','detail','created_at']" empty-text="暂无操作日志" />
            <AdminPager v-model:page="logPage" :total="logTotalPages" :count="filteredOperationLogs.length" />
          </template>
        </main>
      </div>
      <div v-if="!isDesktop" class="admin-profile-chip admin-profile-chip--mobile" aria-label="当前管理员信息">
        <div class="admin-profile-avatar">{{ adminProfileInitial }}</div>
        <div class="admin-profile-meta">
          <strong>{{ me.name || me.account || '管理员' }}</strong>
          <span>{{ adminRoleLabel }}</span>
        </div>
      </div>
    </section>

    <div v-if="confirmReservationOpen" class="modal-mask" @click.self="confirmReservationOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">🪑 确认预约</div>
          <button type="button" class="modal-close" @click="confirmReservationOpen = false">✕</button>
        </div>
        <div v-if="selectedSeat" class="modal-body">
          <div class="summary-row"><span>自习室</span><strong>{{ currentRoom?.name }}</strong></div>
          <div class="summary-row"><span>座位</span><strong>{{ selectedSeat.seat_no }}</strong></div>
          <div class="summary-row"><span>日期</span><strong>{{ reservationForm.date }}</strong></div>
          <div class="summary-row"><span>时段</span><strong>{{ reservationForm.startTime }} - {{ reservationForm.endTime }}</strong></div>
          <div class="summary-row"><span>时长</span><strong>{{ reservationDurationText }}</strong></div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="confirmReservationOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="createReservation">确认预约</button>
        </div>
      </div>
    </div>

    <div v-if="checkoutModalOpen" class="modal-mask" @click.self="checkoutModalOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">🎉 签退成功</div>
          <button type="button" class="modal-close" @click="checkoutModalOpen = false">✕</button>
        </div>
        <div class="modal-body" style="text-align:center">
          <div style="font-size:44px">🎊</div>
          <strong>今日学习完成！</strong>
        </div>
        <div class="card">
          <div class="summary-row"><span>自习室</span><strong>{{ checkoutSummary.roomName }}</strong></div>
          <div class="summary-row"><span>座位号</span><strong>{{ checkoutSummary.seatNo }}</strong></div>
          <div class="summary-row"><span>学习时长</span><strong>{{ checkoutSummary.minutes }} 分钟</strong></div>
          <div class="summary-row"><span>信用积分</span><strong style="color:#00b894">{{ checkoutSummary.creditChange }}</strong></div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-primary" @click="checkoutModalOpen = false">完成</button>
        </div>
      </div>
    </div>

    <div v-if="genericModal.open" class="modal-mask modal-confirm-layer" @click.self="genericModal.open = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">{{ genericModal.title }}</div>
          <button type="button" class="modal-close" @click="genericModal.open = false">✕</button>
        </div>
        <div class="modal-body">{{ genericModal.message }}</div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="genericModal.open = false">取消</button>
          <button type="button" class="btn btn-primary" @click="runGenericConfirm">确定</button>
        </div>
      </div>
    </div>

    <div v-if="adminFormOpen" class="modal-mask" @click.self="adminFormOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">{{ adminForm.id ? '编辑管理员' : '新增管理员' }}</div>
          <button type="button" class="modal-close" @click="adminFormOpen = false">✕</button>
        </div>
        <div class="dialog-form">
          <div class="field"><label>登录账号</label><input v-model="adminForm.account" class="input" :disabled="!!adminForm.id" placeholder="如 lib_admin01" /></div>
          <div class="field"><label>姓名</label><input v-model="adminForm.name" class="input" placeholder="真实姓名" /></div>
          <div class="field"><label>手机号</label><input v-model="adminForm.phone" class="input" placeholder="联系电话" /></div>
          <div class="field">
            <label>角色</label>
            <p v-if="adminForm.isSuperAdmin" class="muted admin-role-fixed">超级管理员（系统内置，不可通过此界面变更）</p>
            <p v-else class="muted admin-role-fixed">普通管理员（图书馆负责人，可分配自习室）</p>
          </div>
          <div class="field"><label>{{ adminForm.id ? '新密码（留空不改）' : '初始密码' }}</label><input v-model="adminForm.password" type="password" class="input" placeholder="6位以上" /></div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="adminFormOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="saveAdminAccount">保存</button>
        </div>
      </div>
    </div>

    <div v-if="userDetailOpen" class="modal-mask" @click.self="userDetailOpen = false">
      <div class="modal-card user-detail-modal">
        <div class="modal-head">
          <div class="modal-title">注册申请详情</div>
          <button type="button" class="modal-close" @click="userDetailOpen = false">✕</button>
        </div>
        <div class="user-detail-grid">
          <div><span>学号</span><strong>{{ userDetail.student_no || userDetail.username || '—' }}</strong></div>
          <div><span>姓名</span><strong>{{ userDetail.name || '—' }}</strong></div>
          <div><span>性别</span><strong>{{ userDetail.gender || '—' }}</strong></div>
          <div><span>学院</span><strong>{{ userDetail.college || '—' }}</strong></div>
          <div><span>专业</span><strong>{{ userDetail.major || '—' }}</strong></div>
          <div><span>年级</span><strong>{{ userDetail.grade || '—' }}</strong></div>
          <div><span>手机</span><strong>{{ userDetail.phone || '—' }}</strong></div>
          <div><span>邮箱</span><strong>{{ userDetail.email || '—' }}</strong></div>
          <div><span>审核状态</span><strong>{{ userDetail.auditLabel || auditStatusLabel(userDetail.audit_status) }}</strong></div>
          <div v-if="userDetail.audit_remark"><span>审核备注</span><strong>{{ userDetail.audit_remark }}</strong></div>
        </div>
        <div class="field user-material-block">
          <label>身份材料</label>
          <img v-if="isImageMaterial(userDetail.material_url)" class="user-material-preview" :src="assetUrl(userDetail.material_url)" alt="身份材料" />
          <a v-else-if="userDetail.material_url" class="user-material-link" :href="assetUrl(userDetail.material_url)" target="_blank" rel="noopener">点击查看上传的材料（PDF/文件）</a>
          <p v-else class="muted">未上传身份材料</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="userDetailOpen = false">关闭</button>
          <button v-if="userDetail.audit_status === 'PENDING'" type="button" class="btn btn-danger" @click="rejectFromDetail">拒绝</button>
          <button v-if="userDetail.audit_status === 'PENDING'" type="button" class="btn btn-primary" @click="approveFromDetail">通过审核</button>
        </div>
      </div>
    </div>

    <div v-if="profileInfoOpen" class="modal-mask" @click.self="profileInfoOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">📝 个人信息</div>
          <button type="button" class="modal-close" @click="profileInfoOpen = false">✕</button>
        </div>
        <div class="dialog-form">
          <el-input v-model="profileForm.name" placeholder="姓名" />
          <el-input v-model="profileForm.phone" placeholder="手机号" />
          <el-input v-model="profileForm.email" placeholder="邮箱" />
          <el-input v-model="profileForm.college" placeholder="学院" />
          <el-input v-model="profileForm.major" placeholder="专业" />
          <el-input v-model="profileForm.grade" placeholder="年级" />
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="profileInfoOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="saveProfileAndClose">保存</button>
        </div>
      </div>
    </div>

    <div v-if="feedbackModalOpen" class="modal-mask" @click.self="feedbackModalOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">💬 问题反馈</div>
          <button type="button" class="modal-close" @click="feedbackModalOpen = false">✕</button>
        </div>
        <div class="field">
          <label>严重程度</label>
          <select v-model="feedbackForm.severity" class="input">
            <option v-for="opt in feedbackSeverityOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div class="field">
          <label>反馈内容</label>
          <textarea v-model="feedbackForm.content" class="input" rows="5" placeholder="请输入你遇到的问题或建议"></textarea>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="feedbackModalOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="submitFeedbackModal">提交反馈</button>
        </div>
      </div>
    </div>

    <div v-if="changePasswordOpen" class="modal-mask" @click.self="changePasswordOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">🔐 修改密码</div>
          <button type="button" class="modal-close" @click="changePasswordOpen = false">✕</button>
        </div>
        <div class="dialog-form">
          <div class="field"><label>原密码</label><input v-model="changePasswordForm.oldPassword" type="password" class="input" placeholder="请输入原密码" /></div>
          <div class="field"><label>新密码</label><input v-model="changePasswordForm.newPassword" type="password" class="input" placeholder="6-20位，含字母和数字" /></div>
          <div class="field"><label>确认新密码</label><input v-model="changePasswordForm.confirmPassword" type="password" class="input" placeholder="请再次输入新密码" /></div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="changePasswordOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="submitChangePassword">保存</button>
        </div>
      </div>
    </div>

    <div v-if="rejectOpen" class="modal-mask" @click.self="rejectOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">拒绝注册申请</div>
          <button type="button" class="modal-close" @click="rejectOpen = false">✕</button>
        </div>
        <div class="field">
          <label>拒绝原因</label>
          <textarea v-model="rejectRemark" class="input" rows="4" placeholder="请填写拒绝原因"></textarea>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="rejectOpen = false">取消</button>
          <button type="button" class="btn btn-danger" @click="confirmReject">确认拒绝</button>
        </div>
      </div>
    </div>

    <div v-if="roomFormOpen" class="modal-mask modal-fullscreen">
      <div class="modal-card modal-fullscreen-card">
        <div class="modal-head modal-fullscreen-head">
          <div class="modal-title">{{ roomForm.id ? '编辑自习室' : '新增自习室' }}</div>
          <button type="button" class="modal-close" aria-label="关闭" @click="closeRoomForm">✕</button>
        </div>
        <p v-if="!isSuperAdmin" class="scanner-hint room-form-hint">可修改您负责的自习室信息；保存后请在下方座位控制区编辑格子属性。</p>
        <p v-else-if="roomForm.id" class="scanner-hint room-form-hint">修改行列数并保存后将同步座位网格；点击格子可编辑座位属性。</p>
        <p v-else class="scanner-hint room-form-hint">请填写基本信息与行列数；首次保存后将显示座位控制图。</p>
        <div class="modal-fullscreen-body room-dialog-form">
          <div class="room-row">
            <div class="field"><label>编号</label><input v-model="roomForm.roomCode" class="input" placeholder="编号" :disabled="!!roomForm.id && !isSuperAdmin" /></div>
            <div class="field"><label>名称</label><input v-model="roomForm.name" class="input" placeholder="名称" /></div>
          </div>
          <div class="room-row">
            <div class="field"><label>位置</label><input v-model="roomForm.location" class="input" placeholder="位置" /></div>
            <div class="field"><label>楼层</label><input v-model="roomForm.floor" class="input" placeholder="楼层" /></div>
          </div>
          <div class="room-row">
            <div class="field"><label>开放开始</label><input v-model="roomForm.openTime" class="input" placeholder="07:00:00" /></div>
            <div class="field"><label>开放结束</label><input v-model="roomForm.closeTime" class="input" placeholder="22:30:00" /></div>
          </div>
          <div class="field"><label>设施（逗号分隔）</label><input v-model="roomForm.facilities" class="input" placeholder="空调,WiFi" /></div>
          <div class="field"><label>分布图地址</label><input v-model="roomForm.layoutImageUrl" class="input" placeholder="上传后自动填入" /></div>
          <div class="upload-row">
            <input type="file" accept="image/*" @change="uploadLayoutImage" />
            <img v-if="roomForm.layoutImageUrl" class="layout-preview" :src="assetUrl(roomForm.layoutImageUrl)" alt="预览" />
          </div>
          <div class="room-row">
            <div class="field"><label>行数</label><input v-model.number="roomForm.rowCount" class="input" type="number" min="1" max="20" /></div>
            <div class="field"><label>列数</label><input v-model.number="roomForm.colCount" class="input" type="number" min="1" max="20" /></div>
          </div>
          <div class="field" v-if="isSuperAdmin">
            <label>负责人（图书馆管理员）</label>
            <el-select v-model="roomForm.managerId" placeholder="请选择负责人" style="width:100%">
              <el-option v-for="a in managerOptions" :key="a.id" :label="`${a.name}（${a.account}）`" :value="a.id" />
            </el-select>
          </div>

          <section class="room-seat-section">
            <div class="room-seat-section-head">
              <h3 class="section-title">座位控制</h3>
              <button v-if="roomForm.id" type="button" class="btn btn-outline btn-sm" @click="addAdminSeat">补全座位</button>
            </div>
            <template v-if="roomForm.id">
              <p class="scanner-hint">点击格子可编辑属性；修改行列数后请先保存自习室以同步网格。删除前请确认无进行中预约。</p>
              <el-input v-model="seatKeyword" placeholder="搜索座位号或行列，如 A-12 / R1-C2" clearable />
              <div class="filter-row user-audit-filters">
                <button v-for="f in seatStatusFilters" :key="f.key" type="button" :class="{ active: seatStatusFilter === f.key }" @click="seatStatusFilter = f.key">{{ f.label }}</button>
              </div>
              <div class="seat-map-grid" :style="{ gridTemplateColumns: `repeat(${seatGridColCount}, minmax(0, 1fr))` }">
                <button
                  v-for="cell in filteredSeatGridCells"
                  :key="cell.id ? `s-${cell.id}` : `p-${cell.row_no}-${cell.col_no}`"
                  type="button"
                  class="cell-grid-btn"
                  :class="seatCellClass(cell)"
                  @click="openSeatEdit(cell)"
                >
                  <div>R{{ cell.row_no }}-C{{ cell.col_no }}</div>
                  <div class="cell-tags">
                    <span v-for="tag in seatCellTags(cell)" :key="tag" class="cell-tag">{{ tag }}</span>
                  </div>
                </button>
              </div>
            </template>
            <p v-else class="scanner-hint room-seat-placeholder">请先保存自习室基本信息，保存后此处将显示座位控制图。</p>
          </section>
        </div>
        <div class="modal-fullscreen-footer">
          <button type="button" class="btn btn-outline" @click="closeRoomForm">取消</button>
          <button type="button" class="btn btn-primary btn-block" @click="saveRoom">保存自习室</button>
        </div>
      </div>
    </div>

    <div v-if="seatEditOpen" class="modal-mask modal-seat-edit-layer" @click.self="seatEditOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">{{ seatEditForm.seat_no || '座位' }} 配置</div>
          <button type="button" class="modal-close" @click="seatEditOpen = false">✕</button>
        </div>
        <div class="dialog-form seat-edit-form">
          <label><input type="checkbox" v-model="seatEditForm.is_seat" /> 座位类单元格</label>
          <label><input type="checkbox" v-model="seatEditForm.has_power" /> 有电源</label>
          <label><input type="checkbox" v-model="seatEditForm.near_window" /> 靠窗</label>
          <label><input type="checkbox" v-model="seatEditForm.quiet_zone" /> 静音区</label>
          <label><input type="checkbox" v-model="seatEditForm.hot_seat" /> 热门座位</label>
          <label><input type="checkbox" v-model="seatEditEnabled" /> 可预约（启用）</label>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-danger" @click="deleteSeatEdit">删除座位</button>
          <button type="button" class="btn btn-outline" @click="seatEditOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="saveSeatEdit">保存</button>
        </div>
      </div>
    </div>

    <div v-if="feedbackHandleOpen" class="modal-mask" @click.self="feedbackHandleOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">处理学生反馈</div>
          <button type="button" class="modal-close" @click="feedbackHandleOpen = false">✕</button>
        </div>
        <div class="dialog-form">
          <p class="muted">学生：{{ feedbackHandleForm.studentName }} · {{ feedbackHandleForm.type }}</p>
          <p>{{ feedbackHandleForm.content }}</p>
          <div class="field"><label>处理说明</label><textarea v-model="feedbackHandleForm.handleResult" class="input" rows="4" placeholder="请填写处理结果，将通知学生"></textarea></div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="feedbackHandleOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="submitFeedbackHandle">确认处理</button>
        </div>
      </div>
    </div>

    <div v-if="revokeViolationOpen" class="modal-mask" @click.self="revokeViolationOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">撤销违约记录</div>
          <button type="button" class="modal-close" @click="revokeViolationOpen = false">✕</button>
        </div>
        <div class="dialog-form">
          <p class="muted">学生：{{ revokeViolationForm.studentName }} · 预约号 {{ revokeViolationForm.reservationNo }}</p>
          <p class="muted">{{ revokeViolationForm.roomName }} · {{ revokeViolationForm.seatNo }} · {{ revokeViolationForm.reserveDate }}</p>
          <p>撤销后将恢复该次违约扣除的信用分，并将预约标记为「已取消」。</p>
          <div class="field"><label>撤销说明（可选）</label><textarea v-model="revokeViolationForm.remark" class="input" rows="3" placeholder="如：学生已说明情况，予以撤销"></textarea></div>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-outline" @click="revokeViolationOpen = false">取消</button>
          <button type="button" class="btn btn-primary" @click="submitRevokeViolation">确认撤销</button>
        </div>
      </div>
    </div>

    <div v-if="aboutOpen" class="modal-mask" @click.self="aboutOpen = false">
      <div class="modal-card">
        <div class="modal-head">
          <div class="modal-title">ℹ️ 关于系统</div>
          <button type="button" class="modal-close" @click="aboutOpen = false">✕</button>
        </div>
        <div class="modal-body" style="text-align:center">
          <div style="font-size:48px">📚</div>
          <p><strong>校园自习室预约管理系统 V1.1</strong></p>
          <p class="muted">华南农业大学 · 数据库课程设计</p>
          <p class="muted">界面依据《原型设计.html》与概要设计文档实现。</p>
        </div>
        <div class="modal-actions">
          <button type="button" class="btn btn-primary" @click="aboutOpen = false">确定</button>
        </div>
      </div>
    </div>

    <div v-if="registerOpen" class="modal-mask" @click.self="registerOpen = false">
      <div class="modal-card register-card">
        <div class="modal-head">
          <div class="modal-title">📝 注册账号</div>
          <button type="button" class="modal-close" @click="registerOpen = false">✕</button>
        </div>
        <div class="field"><label>学号</label><input v-model="registerForm.studentNo" class="input" placeholder="请输入12位学号" /></div>
        <div class="field"><label>姓名</label><input v-model="registerForm.name" class="input" placeholder="请输入真实姓名" /></div>
        <div class="field"><label>性别</label>
          <select v-model="registerForm.gender" class="input"><option>男</option><option>女</option><option>保密</option></select>
        </div>
        <div class="field"><label>学院</label><input v-model="registerForm.college" class="input" /></div>
        <div class="field"><label>专业</label><input v-model="registerForm.major" class="input" placeholder="请输入专业名称" /></div>
        <div class="field"><label>年级</label>
          <select v-model="registerForm.grade" class="input">
            <option>2026级</option><option>2025级</option><option>2024级</option><option>2023级</option>
          </select>
        </div>
        <div class="field"><label>手机号</label><input v-model="registerForm.phone" class="input" placeholder="请输入11位手机号" /></div>
        <div class="field"><label>邮箱</label><input v-model="registerForm.email" class="input" placeholder="请输入校园邮箱" /></div>
        <div class="field"><label>身份材料上传</label><input class="input" type="file" accept=".jpg,.jpeg,.png,.pdf" @change="onRegisterFile" /></div>
        <div class="field"><label>密码</label><input v-model="registerForm.password" type="password" class="input" placeholder="6-20位，包含字母和数字" /></div>
        <div class="field"><label>确认密码</label><input v-model="registerPassword2" type="password" class="input" placeholder="请再次输入密码" /></div>
        <button type="button" class="btn btn-primary btn-block" :disabled="authLoading" @click="register">{{ authLoading ? '提交中…' : '注册' }}</button>
        <p class="muted register-foot">已有账号？<button type="button" class="link-btn" @click="registerOpen = false">立即登录</button></p>
      </div>
    </div>

    <el-dialog v-model="announcementDialog" title="公告" width="min(92vw, 620px)">
      <div class="dialog-form">
        <el-input v-model="announcementForm.title" placeholder="标题" />
        <el-input v-model="announcementForm.content" type="textarea" :rows="5" placeholder="内容" />
        <el-switch v-model="announcementForm.pinned" active-text="置顶" />
      </div>
      <template #footer><el-button type="primary" @click="saveAnnouncement">发布</el-button></template>
    </el-dialog>

    <el-dialog v-model="announcementDetailOpen" :title="activeAnnouncement.title || '公告通知'" width="min(92vw, 620px)">
      <div class="detail-dialog">
        <p>{{ activeAnnouncement.content }}</p>
        <small>{{ formatDate(activeAnnouncement.published_at || activeAnnouncement.created_at) }}</small>
      </div>
    </el-dialog>

    <el-dialog v-model="seatDialogOpen" :title="pendingSeat ? `${pendingSeat.seat_no} 座位配置` : '座位配置'" width="min(92vw, 520px)">
      <div v-if="pendingSeat" class="seat-detail-dialog">
        <div class="seat-config-list">
          <span :class="{ on: pendingSeat.has_power }">插座：{{ pendingSeat.has_power ? '有' : '无' }}</span>
          <span :class="{ on: pendingSeat.near_window }">靠窗：{{ pendingSeat.near_window ? '是' : '否' }}</span>
          <span :class="{ on: pendingSeat.quiet_zone }">静音：{{ pendingSeat.quiet_zone ? '是' : '否' }}</span>
          <span :class="{ on: pendingSeat.hot_seat }">热门：{{ pendingSeat.hot_seat ? '是' : '否' }}</span>
        </div>
        <p>座位类型：{{ pendingSeat.seat_type || '普通座位' }}</p>
        <p>当前状态：{{ pendingSeat.available ? '可预约' : seatUnavailableText(pendingSeat) }}</p>
      </div>
      <template #footer>
        <el-button @click="seatDialogOpen = false">关闭</el-button>
        <el-button type="primary" :disabled="!pendingSeat?.available" @click="confirmSeatSelection">选择该座位</el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import axios from 'axios'
import * as echarts from 'echarts'
import jsQR from 'jsqr'
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { createQrSvg } from './qr'
import {
  ADMIN_COLUMN_LABELS,
  formatAdminCell,
  decorateReservationRow,
  decorateFeedbackRow,
  decorateCheckinRow,
  reservationStatusText
} from './admin-i18n'

const api = axios.create({ baseURL: '/api', timeout: 25000 })
const token = ref(localStorage.getItem('token') || '')
const role = ref(localStorage.getItem('role') || '')
const authLoading = ref(false)
api.interceptors.request.use(config => {
  if (token.value) config.headers.Authorization = `Bearer ${token.value}`
  return config
})
api.interceptors.response.use(res => {
  if (res.data && typeof res.data.code === 'number' && res.data.code !== 200) {
    throw new Error(res.data.message)
  }
  return res
}, err => {
  const status = err.response?.status
  const msg = err.response?.data?.message || err.message || '请求失败'
  if (status === 401 && token.value) {
    clearSession('登录已过期，请重新登录')
  }
  return Promise.reject(new Error(msg))
})

const width = ref(window.innerWidth)
const isDesktop = computed(() => width.value >= 900)
const toast = ref('')
const me = ref({})
const loginRole = ref('student')
const studentLogin = reactive({ username: '202301010101', password: '123456' })
const adminLogin = reactive({ account: 'admin', password: 'admin123' })
const registerOpen = ref(false)
const registerPassword2 = ref('')
const registerForm = reactive({ studentNo: '', name: '', gender: '男', college: '计算机科学与技术学院', major: '软件工程', grade: '2023级', phone: '', email: '', password: '', materialUrl: '' })

const rooms = ref([])
const seats = ref([])
const selectedSeat = ref(null)
const studentPage = ref('home')
const reservationForm = reactive({ date: new Date().toISOString().slice(0, 10), roomId: null, startTime: '09:00', endTime: '11:00' })
const reservations = ref([])
const reservationStatus = ref('ALL')
const announcements = ref([])
const notifications = ref([])
let checkinPollTimer = null
const credit = ref({ score: 0, logs: [] })
const studyStats = ref({})
const studentChart = ref(null)
const statPeriod = ref('day')
const announcementDetailOpen = ref(false)
const activeAnnouncement = ref({})
const seatDialogOpen = ref(false)
const pendingSeat = ref(null)
const seatFilter = ref('all')
const seatFilterOptions = [
  { key: 'all', label: '全部' },
  { key: 'power', label: '有电源' },
  { key: 'window', label: '靠窗' },
  { key: 'quiet', label: '静音' },
  { key: 'hot', label: '热门' }
]
const quickTimeSlots = [
  { label: '08:00-10:00', start: '08:00', end: '10:00' },
  { label: '10:00-12:00', start: '10:00', end: '12:00' },
  { label: '14:00-16:00', start: '14:00', end: '16:00' },
  { label: '19:00-21:00', start: '19:00', end: '21:00' }
]
const RESERVATION_PAST_GRACE_MINUTES = 15
const CREDIT_SCORE_MAX = 500
const feedbackSeverityOptions = [
  { value: 'LOW', label: '低 — 一般建议' },
  { value: 'MEDIUM', label: '中 — 影响使用' },
  { value: 'HIGH', label: '高 — 较严重问题' },
  { value: 'CRITICAL', label: '紧急 — 需立即处理' }
]
const confirmReservationOpen = ref(false)
const checkoutModalOpen = ref(false)
const checkoutSummary = ref({})
const profileInfoOpen = ref(false)
const feedbackModalOpen = ref(false)
const feedbackForm = reactive({ type: 'SUGGESTION', severity: 'MEDIUM', content: '' })
const roomFormOpen = ref(false)
const seatEditOpen = ref(false)
const seatEditForm = reactive({})
const seatEditEnabled = computed({
  get: () => seatEditForm.status === 'NORMAL',
  set: val => { seatEditForm.status = val ? 'NORMAL' : 'DISABLED' }
})
const adminStatsPeriod = ref('week')
const adminStatsRangeMode = ref('current')
const adminStatsRoomId = ref(null)
const userPage = ref(1)
const userPageSize = 10
const ADMIN_LIST_PAGE_SIZE = 10
const adminAccountPage = ref(1)
const reservationPage = ref(1)
const checkinPage = ref(1)
const liveReservationPage = ref(1)
const feedbackPage = ref(1)
const logPage = ref(1)
const roomPage = ref(1)
const announcementPage = ref(1)
const adminAccounts = ref([])
const adminFormOpen = ref(false)
const adminForm = reactive({ id: null, account: '', name: '', phone: '', password: '', isSuperAdmin: false })
const rejectRemark = ref('')
const rejectUserId = ref(null)
const rejectOpen = ref(false)
const userDetailOpen = ref(false)
const userDetail = ref({})
const changePasswordOpen = ref(false)
const changePasswordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const userAuditFilter = ref('')
const userAuditFilters = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待审核' },
  { key: 'APPROVED', label: '已通过' },
  { key: 'REJECTED', label: '已拒绝' }
]
const genericModal = reactive({ open: false, title: '', message: '', onConfirm: null })
const studySeconds = ref(0)
const statAdminView = ref('usage')
const liveReservations = ref([])
const aboutOpen = ref(false)
const notifyPrefs = reactive({ reservation: true, checkin: true, announcement: true, dnd: false })
let studyTimerHandle = null

const adminPage = ref('checkins')
const users = ref([])
const userKeyword = ref('')
const adminReservations = ref([])
const checkins = ref([])
const adminFeedback = ref([])
const adminStatsReport = ref({ summary: {}, usage: [], peak: [], trend: [], credit: [] })
const feedbackHandleOpen = ref(false)
const feedbackHandleForm = reactive({ id: null, studentName: '', type: '', content: '', handleResult: '' })
const adminSeats = ref([])
const scanStudentNo = ref('')
const scanPhotoInput = ref(null)
const scanHint = ref('')
const scanBusy = ref(false)
const checkinQrSvg = ref('')
const usageChart = ref(null)
const roomForm = reactive({})
const announcementDialog = ref(false)
const announcementForm = reactive({})
const profileForm = reactive({ name: '', phone: '', email: '', college: '', major: '', grade: '' })
const operationLogs = ref([])
const adminKeyword = ref('')
const adminStatusFilter = ref('')
const roomKeyword = ref('')
const roomStatusFilter = ref('')
const seatKeyword = ref('')
const seatStatusFilter = ref('')
const reservationKeyword = ref('')
const reservationStatusFilter = ref('')
const reservationRoomFilter = ref(null)
const checkinKeyword = ref('')
const checkinResultFilter = ref('')
const announcementKeyword = ref('')
const feedbackKeyword = ref('')
const feedbackStatusFilter = ref('')
const logKeyword = ref('')
const logModuleFilter = ref('')
const revokeViolationOpen = ref(false)
const revokeViolationForm = reactive({ id: null, studentName: '', reservationNo: '', roomName: '', seatNo: '', reserveDate: '', remark: '' })
const adminStatusFilters = [{ key: '', label: '全部' }, { key: 'NORMAL', label: '正常' }, { key: 'DISABLED', label: '已禁用' }]
const roomStatusFilters = [{ key: '', label: '全部' }, { key: 'OPEN', label: '开放' }, { key: 'CLOSED', label: '关闭' }, { key: 'MAINTENANCE', label: '维护中' }]
const seatStatusFilters = [{ key: '', label: '全部' }, { key: 'NORMAL', label: '正常' }, { key: 'DAMAGED', label: '损坏' }, { key: 'DISABLED', label: '禁用' }]
const reservationAdminStatusFilters = [
  { key: '', label: '全部' }, { key: 'PENDING', label: '待使用' }, { key: 'USING', label: '使用中' },
  { key: 'COMPLETED', label: '已完成' }, { key: 'CANCELLED', label: '已取消' }, { key: 'VIOLATED', label: '违约' }
]
const checkinResultFilters = [{ key: '', label: '全部' }, { key: 'ON_TIME', label: '准时' }, { key: 'LATE', label: '迟到' }, { key: 'INVALID', label: '无效' }]
const feedbackStatusFilters = [{ key: '', label: '全部' }, { key: 'PENDING', label: '待处理' }, { key: 'PROCESSING', label: '处理中' }, { key: 'DONE', label: '已处理' }]
const logModuleFilters = [{ key: '', label: '全部' }, { key: 'USER', label: '用户' }, { key: 'ROOM', label: '自习室' }, { key: 'RESERVATION', label: '预约' }, { key: 'FEEDBACK', label: '反馈' }]
const tempLeaveHint = ref('暂离中，请在 30 分钟内返回座位')

const todayText = computed(() => new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' }))
const studentNoDisplay = computed(() => me.value.student_no || me.value.username || '—')
const homeDateText = computed(() => `${new Date().getMonth() + 1}月${new Date().getDate()}日`)
const unreadCount = computed(() => notifications.value.filter(n => !n.read_flag).length)
const currentRoom = computed(() => rooms.value.find(r => r.id === reservationForm.roomId))
const seatGridRoom = computed(() => {
  if (!roomFormOpen.value || !roomForm.id) return null
  const r = rooms.value.find(x => Number(x.id) === Number(roomForm.id))
  return {
    ...r,
    col_count: roomForm.colCount || r?.col_count || r?.colCount || 6,
    row_count: roomForm.rowCount || r?.row_count || r?.rowCount || 4
  }
})
const seatGridColCount = computed(() => Math.max(1, Number(seatGridRoom.value?.col_count || seatGridRoom.value?.colCount || roomForm.colCount || 6)))
const seatGridRowCount = computed(() => Math.max(1, Number(seatGridRoom.value?.row_count || seatGridRoom.value?.rowCount || roomForm.rowCount || 4)))
const seatGridCells = computed(() => {
  const rows = seatGridRowCount.value
  const cols = seatGridColCount.value
  const byPos = {}
  for (const s of adminSeats.value) {
    byPos[`${s.row_no}-${s.col_no}`] = s
  }
  const cells = []
  for (let r = 1; r <= rows; r++) {
    for (let c = 1; c <= cols; c++) {
      const found = byPos[`${r}-${c}`]
      cells.push(found || {
        row_no: r,
        col_no: c,
        is_seat: 1,
        status: 'NORMAL',
        seat_no: `R${r}-C${c}`,
        placeholder: true
      })
    }
  }
  return cells
})
const todayReservation = computed(() => reservations.value.find(r => String(r.reserve_date).startsWith(reservationForm.date) && ['PENDING', 'USING'].includes(r.status)))
const activeReservation = computed(() => reservations.value.find(r => ['PENDING', 'USING', 'TEMP_LEAVE'].includes(r.status)))
function parseReservationDateTime(r, timeField = 'start_time') {
  if (!r) return null
  const rawDate = r.reserve_date ?? r.reserveDate
  let datePart = ''
  if (rawDate instanceof Date) {
    datePart = rawDate.toISOString().slice(0, 10)
  } else {
    datePart = String(rawDate || '').replace(' ', 'T').slice(0, 10)
  }
  const altTime = timeField === 'start_time' ? 'startTime' : 'endTime'
  const rawTime = r[timeField] ?? r[altTime] ?? '00:00:00'
  const timePart = String(rawTime).slice(0, 8)
  const [y, mo, d] = datePart.split('-').map(n => Number(n))
  const [hh, mm, ss = 0] = timePart.split(':').map(n => Number(n))
  if (!y || !mo || !d) return null
  return new Date(y, mo - 1, d, hh || 0, mm || 0, ss || 0)
}
function reservationStartDate(r) {
  return parseReservationDateTime(r, 'start_time')
}
function isWithinCheckinWindow(r) {
  const start = reservationStartDate(r)
  if (!start || Number.isNaN(start.getTime())) return false
  const windowStart = start.getTime() - 15 * 60 * 1000
  const windowEnd = start.getTime() + 15 * 60 * 1000
  const now = Date.now()
  return now >= windowStart && now <= windowEnd
}
const checkinWindowHint = computed(() => {
  const r = activeReservation.value
  if (!r || r.status !== 'PENDING') return ''
  const start = reservationStartDate(r)
  if (!start) return '请在预约开始前后 15 分钟内完成签到。'
  const windowStart = new Date(start.getTime() - 15 * 60 * 1000)
  const windowEnd = new Date(start.getTime() + 15 * 60 * 1000)
  const fmt = (dt) => dt.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  if (Date.now() < windowStart.getTime()) {
    return `签到尚未开始，开放时间：${fmt(windowStart)} 至 ${fmt(windowEnd)}`
  }
  if (Date.now() > windowEnd.getTime()) {
    return '签到时间已过，请等待系统处理或联系管理员。'
  }
  return '请在预约开始前后 15 分钟内完成签到。'
})
const timerText = computed(() => {
  if (!activeReservation.value) return '00:00:00'
  if (activeReservation.value.status === 'TEMP_LEAVE') {
    const leaveAt = activeReservation.value.tempLeaveTime || activeReservation.value.temp_leave_time
    if (!leaveAt) return '暂离中'
    const maxMin = Number(activeReservation.value.maxLeaveMinutes || activeReservation.value.max_leave_minutes || 30)
    const start = new Date(String(leaveAt).replace(' ', 'T'))
    const remain = Math.max(0, maxMin * 60 - Math.floor((Date.now() - start.getTime()) / 1000))
    const h = String(Math.floor(remain / 3600)).padStart(2, '0')
    const m = String(Math.floor((remain % 3600) / 60)).padStart(2, '0')
    const s = String(remain % 60).padStart(2, '0')
    return `${h}:${m}:${s}`
  }
  if (activeReservation.value.status === 'USING') return formatStudyTime(studySeconds.value)
  return '00:00:00'
})
const groupedSeats = computed(() => {
  const order = ['热门区', '静音区', '开放座位', '非座位区']
  const map = {}
  for (const s of seats.value) {
    const name = resolveSeatSection(s)
    if (!map[name]) map[name] = []
    map[name].push(s)
  }
  return order.filter(name => map[name]?.length).map(name => ({
    name,
    seats: map[name],
    availableCount: map[name].filter(s => s.is_seat && s.available).length
  }))
})
const studentTitle = computed(() => ({ home: '首页', reservation: '座位预约', checkin: '签到签退', profile: '我的', myres: '我的预约', credit: '信用积分', stats: '学习统计', notifications: '消息通知', settings: '设置', feedback: '问题反馈' }[studentPage.value] || '首页'))
const shownReservations = computed(() => reservationStatus.value === 'ALL' ? reservations.value : reservations.value.filter(r => r.status === reservationStatus.value))
const availableSeatCount = computed(() => seats.value.filter(s => s.available).length)
const roomLayoutImage = computed(() => currentRoom.value?.layout_image_url || currentRoom.value?.layoutImageUrl || '')
const RESERVATION_SLOT_STEP_MINUTES = 10
/** 从自习室实体读取 HH:mm（兼容 open_time / openTime） */
function roomTimePart(room, field, fallback) {
  if (!room) return fallback
  const camel = field.replace(/_([a-z])/g, (_, c) => c.toUpperCase())
  const raw = room[field] ?? room[camel] ?? fallback
  return String(raw).slice(0, 5)
}
const currentRoomOpenTime = computed(() => roomTimePart(currentRoom.value, 'open_time', '07:00'))
const currentRoomCloseTime = computed(() => roomTimePart(currentRoom.value, 'close_time', '22:30'))
/** 最晚开始 = 关闭时间 - 一个时段步长（如 23:30 关则可约 23:20 起） */
const latestReservationStartTime = computed(() => {
  const closeM = timeToMinutes(currentRoomCloseTime.value)
  return minutesToTime(Math.max(0, closeM - RESERVATION_SLOT_STEP_MINUTES))
})
const endSelectStart = computed(() => addMinutes(reservationForm.startTime, RESERVATION_SLOT_STEP_MINUTES))
const todayDateValue = computed(() => toDateValue(new Date()))
const minStartTimeToday = computed(() => {
  const cutoff = new Date(Date.now() - RESERVATION_PAST_GRACE_MINUTES * 60 * 1000)
  let mins = cutoff.getHours() * 60 + cutoff.getMinutes()
  mins = Math.ceil(mins / RESERVATION_SLOT_STEP_MINUTES) * RESERVATION_SLOT_STEP_MINUTES
  if (mins >= 24 * 60) return latestReservationStartTime.value
  const todayMin = minutesToTime(mins)
  return minutesToTime(Math.max(timeToMinutes(currentRoomOpenTime.value), timeToMinutes(todayMin)))
})
const startTimeSelectMin = computed(() => (
  reservationForm.date === todayDateValue.value ? minStartTimeToday.value : currentRoomOpenTime.value
))
const startTimeOptions = computed(() => buildReservationTimeOptions(
  startTimeSelectMin.value, latestReservationStartTime.value, RESERVATION_SLOT_STEP_MINUTES))
const endTimeOptions = computed(() => buildReservationTimeOptions(
  endSelectStart.value, currentRoomCloseTime.value, RESERVATION_SLOT_STEP_MINUTES))
const availableQuickTimeSlots = computed(() => {
  const openM = timeToMinutes(currentRoomOpenTime.value)
  const closeM = timeToMinutes(currentRoomCloseTime.value)
  return quickTimeSlots.map(slot => {
    const startM = timeToMinutes(slot.start)
    const endM = timeToMinutes(slot.end)
    const outOfRoom = startM < openM || endM > closeM
    return {
      ...slot,
      expired: isQuickSlotExpired(slot) || outOfRoom
    }
  })
})
const dateOptions = computed(() => Array.from({ length: 7 }, (_, i) => {
  const d = new Date()
  d.setDate(d.getDate() + i)
  return {
    date: toDateValue(d),
    label: i === 0 ? '今天' : ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()],
    day: String(d.getDate()).padStart(2, '0'),
    month: d.getMonth() + 1
  }
}))
const activeStudentTab = computed(() => ['myres', 'credit', 'stats', 'settings', 'feedback'].includes(studentPage.value) ? 'profile' : studentPage.value)
const creditBlocked = computed(() => Number(credit.value.score ?? me.value.credit_score ?? 300) <= 0)
const creditPercent = computed(() => Math.min(100, Math.round(Number(credit.value.score || 0) / CREDIT_SCORE_MAX * 100)))
const creditLevel = computed(() => Number(credit.value.score || 0) >= 280 ? '优秀' : Number(credit.value.score || 0) >= 200 ? '良好' : '需改进')
const checkinCount = computed(() => credit.value.logs?.filter(l => String(l.reason || '').includes('签到')).length || 0)
const violationCount = computed(() => reservations.value.filter(r => ['VIOLATED', 'AUTO_CANCELLED'].includes(r.status)).length)
const totalStudyHours = computed(() => ((studyStats.value.totalMinutes || 0) / 60).toFixed(1).replace('.0', ''))
const studyDays = computed(() => studyStats.value.series?.length || 0)
const averageStudyHours = computed(() => {
  const days = Math.max(1, studyDays.value)
  return (((studyStats.value.totalMinutes || 0) / 60) / days).toFixed(1).replace('.0', '')
})
const studyChartTitle = computed(() => ({
  day: '今日各时段学习时长（小时）',
  week: '本周每日学习时长（小时）',
  month: '本月每周学习时长（小时）',
  year: '近一年每月学习时长（小时）'
}[statPeriod.value] || '学习时长（小时）'))
const studyBars = computed(() => {
  const rows = studyStats.value.series || []
  if (rows.length) {
    return rows.map(row => ({
      label: formatStudyLabel(row.label),
      value: Number(((Number(row.minutes || 0)) / 60).toFixed(1))
    }))
  }
  if (statPeriod.value === 'day') {
    return [8, 10, 12, 14, 16, 18, 20, 22].map(hour => ({ label: `${hour}时`, value: 0 }))
  }
  const days = statPeriod.value === 'month' ? 30 : 7
  return Array.from({ length: days }, (_, i) => ({ label: `${i + 1}`, value: 0 }))
})
const maxStudyBarValue = computed(() => Math.max(1, ...studyBars.value.map(b => Number(b.value || 0))))
const studyAdvice = computed(() => {
  if (statPeriod.value === 'day') {
    return Number(totalStudyHours.value) >= 3 ? '你今日的学习效率较高，建议保持。最佳学习时段为上午9-11点。' : '建议增加连续学习时长，并优先预约上午或下午的稳定时段。'
  }
  return Number(averageStudyHours.value) >= 2 ? '本周期学习节奏较稳定，建议继续保持固定预约习惯。' : '建议提高每次学习的连续时长，并保持每周稳定到馆。'
})

const statPeriods = [{ key: 'day', label: '日报' }, { key: 'week', label: '周报' }, { key: 'month', label: '月报' }, { key: 'year', label: '年报' }]
const reservationTabs = [{ key: 'ALL', label: '全部' }, { key: 'PENDING', label: '待使用' }, { key: 'USING', label: '使用中' }, { key: 'COMPLETED', label: '已完成' }, { key: 'CANCELLED', label: '已取消' }]
const studentNav = [{ page: 'home', label: '首页', icon: '🏠' }, { page: 'reservation', label: '预约', icon: '🪑' }, { page: 'checkin', label: '签到', icon: '✅' }, { page: 'profile', label: '我的', icon: '👤' }]
const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')
const adminRoleLabel = computed(() => {
  if (role.value === 'SUPER_ADMIN') return '超级管理员'
  if (role.value === 'ADMIN') return '普通管理员'
  return role.value || '管理员'
})
const adminProfileInitial = computed(() => {
  const name = String(me.value.name || me.value.account || '管').trim()
  return name.slice(0, 1).toUpperCase()
})
const adminNav = computed(() => {
  const items = [
    { page: 'checkins', label: '签到', icon: '✅' },
    { page: 'users', label: '用户管理', icon: '👥' }
  ]
  if (isSuperAdmin.value) {
    items.push({ page: 'admins', label: '管理员管理', icon: '🛡️' })
  }
  items.push(
    { page: 'rooms', label: '自习室', icon: '🏫' },
    { page: 'reservations', label: '预约', icon: '📅' },
    { page: 'announcements', label: '公告', icon: '📣' },
    { page: 'statistics', label: '统计', icon: '📈' },
    { page: 'feedback', label: '反馈', icon: '💬' },
    { page: 'settings', label: '设置', icon: '⚙️' }
  )
  return items
})
const adminAccountColumns = ['account', 'name', 'roleLabel', 'phone', 'managedRooms', 'statusLabel']
const managerOptions = computed(() => adminAccounts.value.filter(a => a.role === 'ADMIN' && a.status !== 'DISABLED'))
const decoratedAdminAccounts = computed(() => adminAccounts.value.map(row => ({
  ...row,
  roleLabel: row.role === 'SUPER_ADMIN' ? '超级管理员' : '普通管理员',
  statusLabel: row.status === 'DISABLED' ? '已禁用' : '正常',
  managedRooms: row.managedRooms || '—'
})))
function paginateRows(rows, page, pageSize = ADMIN_LIST_PAGE_SIZE) {
  const total = pagerTotal(rows.length, pageSize)
  const safePage = Math.min(Math.max(1, page), total)
  const start = (safePage - 1) * pageSize
  return rows.slice(start, start + pageSize)
}
function pagerTotal(count, pageSize = ADMIN_LIST_PAGE_SIZE) {
  return Math.max(1, Math.ceil(Math.max(0, count) / pageSize))
}
function matchAdminKeyword(row, keyword, fields) {
  const q = String(keyword || '').trim().toLowerCase()
  if (!q) return true
  return fields.some(f => String(row[f] ?? '').toLowerCase().includes(q))
}
const filteredAdminAccounts = computed(() => {
  let rows = decoratedAdminAccounts.value
  if (adminStatusFilter.value) rows = rows.filter(r => r.status === adminStatusFilter.value)
  return rows.filter(r => matchAdminKeyword(r, adminKeyword.value, ['account', 'name', 'phone', 'managedRooms']))
})
const pagedAdminAccounts = computed(() => paginateRows(filteredAdminAccounts.value, adminAccountPage.value))
const adminAccountTotalPages = computed(() => pagerTotal(filteredAdminAccounts.value.length))
const filteredRooms = computed(() => {
  let rows = rooms.value
  if (roomStatusFilter.value) rows = rows.filter(r => r.status === roomStatusFilter.value)
  return rows.filter(r => matchAdminKeyword(r, roomKeyword.value, ['name', 'location', 'floor', 'room_code']))
})
const pagedRooms = computed(() => paginateRows(filteredRooms.value, roomPage.value))
const roomTotalPages = computed(() => pagerTotal(filteredRooms.value.length))
const filteredSeatGridCells = computed(() => {
  let cells = seatGridCells.value
  if (seatStatusFilter.value) cells = cells.filter(c => String(c.status || 'NORMAL') === seatStatusFilter.value)
  const q = String(seatKeyword.value || '').trim().toLowerCase()
  if (!q) return cells
  return cells.filter(c => {
    const label = `R${c.row_no}-C${c.col_no} ${c.seat_no || ''}`.toLowerCase()
    return label.includes(q)
  })
})
const sortedAnnouncements = computed(() => [...announcements.value].sort((a, b) => Number(b.pinned || 0) - Number(a.pinned || 0)))
const pagedUsers = computed(() => {
  const start = (userPage.value - 1) * userPageSize
  return users.value.slice(start, start + userPageSize)
})
const userTotalPages = computed(() => Math.max(1, Math.ceil(users.value.length / userPageSize)))
const decoratedLiveReservations = computed(() =>
  (liveReservations.value || []).map(r => ({
    ...r,
    reserveDate: formatDate(r.reserveDate || r.reserve_date),
    status: reservationStatusText(r.status)
  }))
)
const decoratedAdminReservations = computed(() => adminReservations.value.map(r => ({
  ...decorateReservationRow(r),
  _rawStatus: r.status,
  _rawId: r.id,
  cancel_reason: r.cancel_reason || '—'
})))
const filteredAdminReservations = computed(() => {
  let rows = decoratedAdminReservations.value
  if (reservationStatusFilter.value) rows = rows.filter(r => r._rawStatus === reservationStatusFilter.value)
  if (reservationRoomFilter.value) rows = rows.filter(r => Number(r.room_id) === Number(reservationRoomFilter.value))
  return rows.filter(r => matchAdminKeyword(r, reservationKeyword.value, [
    'student_no', 'studentNo', 'studentName', 'roomName', 'seatNo', 'reservation_no'
  ]))
})
const pagedAdminReservations = computed(() => paginateRows(filteredAdminReservations.value, reservationPage.value))
const reservationTotalPages = computed(() => pagerTotal(filteredAdminReservations.value.length))
const decoratedCheckins = computed(() => checkins.value.map(r => ({ ...decorateCheckinRow(r), _rawResult: r.result })))
const filteredCheckins = computed(() => {
  let rows = decoratedCheckins.value
  if (checkinResultFilter.value) rows = rows.filter(r => r._rawResult === checkinResultFilter.value)
  return rows.filter(r => matchAdminKeyword(r, checkinKeyword.value, ['studentNo', 'studentName', 'roomName', 'seatNo']))
})
const pagedCheckins = computed(() => paginateRows(filteredCheckins.value, checkinPage.value))
const checkinTotalPages = computed(() => pagerTotal(filteredCheckins.value.length))
const pagedLiveReservations = computed(() => paginateRows(decoratedLiveReservations.value, liveReservationPage.value))
const liveReservationTotalPages = computed(() => pagerTotal(decoratedLiveReservations.value.length))
const filteredAnnouncements = computed(() => sortedAnnouncements.value.filter(a =>
  matchAdminKeyword(a, announcementKeyword.value, ['title', 'content'])
))
const pagedAnnouncements = computed(() => paginateRows(filteredAnnouncements.value, announcementPage.value))
const announcementTotalPages = computed(() => pagerTotal(filteredAnnouncements.value.length))
const decoratedAdminFeedback = computed(() => adminFeedback.value.map(row => {
  const d = decorateFeedbackRow(row)
  return { ...d, _rawStatus: row.status }
}))
const filteredAdminFeedback = computed(() => {
  let rows = decoratedAdminFeedback.value
  if (feedbackStatusFilter.value) rows = rows.filter(r => r._rawStatus === feedbackStatusFilter.value)
  return rows.filter(r => matchAdminKeyword(r, feedbackKeyword.value, ['studentName', 'studentNo', 'type', 'content', 'roomName']))
})
const pagedAdminFeedback = computed(() => paginateRows(filteredAdminFeedback.value, feedbackPage.value))
const feedbackTotalPages = computed(() => pagerTotal(filteredAdminFeedback.value.length))
const filteredOperationLogs = computed(() => {
  let rows = operationLogs.value
  if (logModuleFilter.value) rows = rows.filter(r => String(r.module || '') === logModuleFilter.value)
  return rows.filter(r => matchAdminKeyword(r, logKeyword.value, ['module', 'action', 'target_type', 'detail', 'operator_name']))
})
const pagedOperationLogs = computed(() => paginateRows(filteredOperationLogs.value, logPage.value))
const logTotalPages = computed(() => pagerTotal(filteredOperationLogs.value.length))
const adminStatSummary = computed(() => {
  const s = adminStatsReport.value.summary || {}
  return {
    totalReserve: s.totalReserve || 0,
    usingCount: s.usingCount || 0,
    checkinRate: s.checkinRate || 0,
    avgCredit: s.avgCredit || 0
  }
})
const adminStatsScopeLabel = computed(() => {
  if (!adminStatsRoomId.value) return '全部自习室'
  const room = rooms.value.find(r => Number(r.id) === Number(adminStatsRoomId.value))
  return room?.name || '指定自习室'
})
const reservationDurationText = computed(() => {
  if (!reservationForm.startTime || !reservationForm.endTime) return ''
  const mins = timeToMinutes(reservationForm.endTime) - timeToMinutes(reservationForm.startTime)
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h ? `${h} 小时${m ? ` ${m} 分钟` : ''}` : `${m} 分钟`
})

function notify(message) {
  toast.value = message
  setTimeout(() => { toast.value = '' }, 2200)
}
function assetUrl(path) {
  if (!path) return ''
  if (String(path).startsWith('http')) return path
  return String(path).startsWith('/') ? path : `/${path}`
}
function isImageMaterial(url) {
  return /\.(jpg|jpeg|png|gif|webp|bmp)(\?.*)?$/i.test(String(url || ''))
}
function statCount(row) {
  return Number(row?.cnt ?? row?.count ?? 0)
}
/** ECharts 坐标轴：次数类指标 */
function countYAxis(name = '预约次数（次）') {
  return {
    type: 'value',
    name,
    nameTextStyle: { fontSize: 12, color: '#667085' },
    axisLabel: { formatter: (v) => `${v} 次` }
  }
}
/** ECharts 坐标轴：百分比类指标 */
function percentYAxis(name = '使用率（%）') {
  return {
    type: 'value',
    name,
    nameTextStyle: { fontSize: 12, color: '#667085' },
    axisLabel: { formatter: (v) => `${v}%` }
  }
}
/** ECharts 坐标轴：学习时长（小时） */
function hourYAxis(name = '学习时长（小时）') {
  return {
    type: 'value',
    name,
    nameTextStyle: { fontSize: 12, color: '#667085' },
    axisLabel: { formatter: (v) => `${v} h` }
  }
}
function countTooltip() {
  return { trigger: 'axis', valueFormatter: (v) => `${v} 次` }
}
function hourTooltip() {
  return { trigger: 'axis', valueFormatter: (v) => `${v} 小时` }
}
function trendAxisLabel(row) {
  if (row?.peakHour != null && row.peakHour !== '') {
    return `${String(row.peakHour).padStart(2, '0')}:00`
  }
  if (row?.monthNum != null && row.monthNum !== '') {
    return `${row.monthNum}月`
  }
  return row?.timeLabel ?? row?.label ?? ''
}
function peakAxisLabel(row) {
  const h = row?.peakHour ?? row?.hour
  return h != null && h !== '' ? `${h}时` : ''
}
function isQuickSlotExpired(slot) {
  if (reservationForm.date !== todayDateValue.value) return false
  const cutoff = Date.now() - RESERVATION_PAST_GRACE_MINUTES * 60 * 1000
  const [h, m] = slot.start.split(':').map(Number)
  const slotStart = new Date()
  slotStart.setHours(h, m, 0, 0)
  return slotStart.getTime() < cutoff
}
function clampReservationStartForToday() {
  if (reservationForm.date !== todayDateValue.value) {
    normalizeReservationTimes()
    return
  }
  if (timeToMinutes(reservationForm.startTime) < timeToMinutes(minStartTimeToday.value)) {
    reservationForm.startTime = minStartTimeToday.value
    if (timeToMinutes(reservationForm.endTime) <= timeToMinutes(reservationForm.startTime)) {
      reservationForm.endTime = addMinutes(reservationForm.startTime, 120)
    }
  }
  normalizeReservationTimes()
}
function ensureReservationTimeAllowed() {
  if (timeToMinutes(reservationForm.startTime) < timeToMinutes(currentRoomOpenTime.value)) {
    throw new Error(`开始时间不能早于自习室开放时间 ${currentRoomOpenTime.value}`)
  }
  if (timeToMinutes(reservationForm.endTime) > timeToMinutes(currentRoomCloseTime.value)) {
    throw new Error(`结束时间不能晚于自习室关闭时间 ${currentRoomCloseTime.value}`)
  }
  if (reservationForm.date !== todayDateValue.value) return
  const cutoff = Date.now() - RESERVATION_PAST_GRACE_MINUTES * 60 * 1000
  const [h, m] = reservationForm.startTime.split(':').map(Number)
  const start = new Date()
  start.setHours(h, m, 0, 0)
  if (start.getTime() < cutoff) {
    throw new Error('不能预约已开始超过15分钟的时段，请选择更晚的开始时间')
  }
}
function roomStatusText(status) {
  return { OPEN: '开放', CLOSED: '关闭', MAINTENANCE: '维护中' }[status] || status || '-'
}
function stopCheckinPagePoll() {
  if (checkinPollTimer) {
    clearInterval(checkinPollTimer)
    checkinPollTimer = null
  }
}
/** 签到页轮询：管理员按学号签到后自动切到「使用中」 */
async function syncCheckinStateFromServer() {
  if (studentPage.value !== 'checkin') return
  const tracked = activeReservation.value
  if (!tracked?.id) return
  const prevId = tracked.id
  const prevStatus = tracked.status
  try {
    await loadReservations()
    const updated = reservations.value.find(r => Number(r.id) === Number(prevId))
    if (!updated) return
    if (prevStatus === 'PENDING' && ['USING', 'TEMP_LEAVE'].includes(updated.status)) {
      updateStudyTimer()
      notify('签到成功，已进入学习状态')
    }
    if (prevStatus === 'USING' && updated.status === 'PENDING') {
      notify('签到无效，已恢复为待签到')
    }
  } catch { /* 轮询失败忽略 */ }
}
function startCheckinPagePoll() {
  stopCheckinPagePoll()
  syncCheckinStateFromServer()
  checkinPollTimer = setInterval(syncCheckinStateFromServer, 2000)
}
function forgetPassword() {
  notify('请联系管理员重置密码，或通过注册邮箱找回')
}
function parseRoomFacilities(room) {
  const raw = room?.facilities
  if (Array.isArray(raw)) return raw
  if (!raw) return []
  return String(raw).split(',').map(x => x.trim()).filter(Boolean)
}
function seatCellClass(seat) {
  if (seat?.placeholder) return 'placeholder'
  if (!seat?.is_seat) return 'nonseat'
  if (seat.has_power) return 'power'
  return ''
}
function seatCellTags(seat) {
  const tags = []
  if (seat?.seat_type) tags.push(seat.seat_type)
  if (seat?.has_power) tags.push('电源')
  if (seat?.near_window) tags.push('靠窗')
  if (seat?.quiet_zone) tags.push('静音')
  if (seat?.hot_seat) tags.push('热门')
  if (!seat?.is_seat) tags.push('非座位')
  return tags.length ? tags : ['普通']
}
function openRoomFormCreate() {
  editRoom()
}
function closeRoomForm() {
  roomFormOpen.value = false
  adminSeats.value = []
  seatKeyword.value = ''
  seatStatusFilter.value = ''
}
function openSeatEdit(seat) {
  if (!seat?.id) {
    notify('该格子暂无座位数据，请先在「自习室管理」保存行列数以同步网格')
    return
  }
  Object.assign(seatEditForm, {
    id: seat.id,
    seat_no: seat.seat_no,
    is_seat: !!seat.is_seat,
    has_power: !!seat.has_power,
    near_window: !!seat.near_window,
    quiet_zone: !!seat.quiet_zone,
    hot_seat: !!seat.hot_seat,
    status: seat.status || 'NORMAL',
    cell_category: seat.cell_category,
    seat_type: seat.seat_type
  })
  seatEditOpen.value = true
}
async function saveSeatEdit() {
  try {
    await call('put', `/admin/seats/${seatEditForm.id}`, {
      isSeat: seatEditForm.is_seat ? 1 : 0,
      hasPower: seatEditForm.has_power ? 1 : 0,
      nearWindow: seatEditForm.near_window ? 1 : 0,
      quietZone: seatEditForm.quiet_zone ? 1 : 0,
      hotSeat: seatEditForm.hot_seat ? 1 : 0,
      status: seatEditForm.status,
      cellCategory: seatEditForm.cell_category || (seatEditForm.is_seat ? 'SEAT' : 'NON_SEAT'),
      seatType: seatEditForm.seat_type || '普通座位'
    })
    seatEditOpen.value = false
    notify('座位配置已保存')
    await loadAdminSeats()
  } catch (e) { notify(e.message) }
}
async function saveProfileAndClose() {
  await saveProfile()
  profileInfoOpen.value = false
}
async function submitFeedbackModal() {
  if (!feedbackForm.content.trim()) {
    notify('请输入反馈内容')
    return
  }
  try {
    await call('post', '/feedback', {
      content: feedbackForm.content,
      type: feedbackForm.type,
      severity: feedbackForm.severity,
      roomId: reservationForm.roomId,
      seatId: selectedSeat.value?.id || activeReservation.value?.seatId
    })
    feedbackModalOpen.value = false
    feedbackForm.content = ''
    notify('反馈已提交')
  } catch (e) { notify(e.message) }
}
function runGenericConfirm() {
  if (typeof genericModal.onConfirm === 'function') genericModal.onConfirm()
  genericModal.open = false
}
async function changeAdminStatsPeriod(period) {
  adminStatsPeriod.value = period
  await loadAdminStatistics()
}
async function changeAdminStatsRangeMode(mode) {
  adminStatsRangeMode.value = mode
  await loadAdminStatistics()
}
async function loadLiveReservations() {
  try {
    liveReservations.value = await call('get', '/admin/live-reservations')
  } catch (e) {
    liveReservations.value = []
    notify(e.message)
  }
}
function toDateValue(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
function formatDate(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}
function formatStudyLabel(value) {
  const text = String(value || '')
  if (statPeriod.value === 'day') return `${String(text).padStart(2, '0')}时`
  return text.length >= 10 ? text.slice(5, 10).replace('-', '/') : text
}
function barHeight(value) {
  if (!Number(value)) return 4
  return Math.max(8, Math.round((Number(value) / maxStudyBarValue.value) * 100))
}
function timeRangeText(item) {
  return `${String(item.start_time || item.startTime || '').slice(0, 5)}-${String(item.end_time || item.endTime || '').slice(0, 5)}`
}
function timeToMinutes(value) {
  const [h, m] = String(value || '00:00').split(':').map(Number)
  return h * 60 + m
}
function minutesToTime(minutes) {
  const safe = Math.max(0, Math.min(23 * 60 + 59, minutes))
  return `${String(Math.floor(safe / 60)).padStart(2, '0')}:${String(safe % 60).padStart(2, '0')}`
}
function addMinutes(value, minutes) {
  return minutesToTime(timeToMinutes(value) + minutes)
}
function ensureEndAfterStart() {
  if (!reservationForm.startTime) return
  if (!reservationForm.endTime || timeToMinutes(reservationForm.endTime) <= timeToMinutes(reservationForm.startTime)) {
    reservationForm.endTime = endSelectStart.value
  }
  if (timeToMinutes(reservationForm.endTime) > timeToMinutes(currentRoomCloseTime.value)) {
    reservationForm.endTime = currentRoomCloseTime.value
  }
}
function formatStudyTime(sec) {
  const h = String(Math.floor(sec / 3600)).padStart(2, '0')
  const m = String(Math.floor((sec % 3600) / 60)).padStart(2, '0')
  const s = String(sec % 60).padStart(2, '0')
  return `${h}:${m}:${s}`
}
function updateStudyTimer() {
  const r = activeReservation.value
  if (!r || r.status !== 'USING' || !r.sign_in_time) {
    studySeconds.value = 0
    return
  }
  const start = new Date(String(r.sign_in_time).replace(' ', 'T'))
  studySeconds.value = Math.max(0, Math.floor((Date.now() - start.getTime()) / 1000))
}
/** 学生端分区：仅保留热门/静音/开放座位，静音单独成区，不再使用精品区、标准区或复合标签 */
function resolveSeatSection(seat) {
  if (!seat.is_seat) return '非座位区'
  if (seat.quiet_zone) return '静音区'
  if (seat.hot_seat) return '热门区'
  return '开放座位'
}
function buildReservationTimeOptions(rangeStart, rangeEnd, stepMin = 10) {
  const options = []
  let cur = timeToMinutes(rangeStart)
  const max = timeToMinutes(rangeEnd)
  while (cur <= max) {
    options.push(minutesToTime(cur))
    cur += stepMin
  }
  return options
}
function normalizeReservationTimes() {
  const starts = startTimeOptions.value
  if (starts.length && !starts.includes(reservationForm.startTime)) {
    reservationForm.startTime = starts[0]
  }
  if (!reservationForm.endTime || timeToMinutes(reservationForm.endTime) <= timeToMinutes(reservationForm.startTime)) {
    reservationForm.endTime = endSelectStart.value
  }
  const ends = endTimeOptions.value
  if (ends.length && !ends.includes(reservationForm.endTime)) {
    reservationForm.endTime = ends.includes(endSelectStart.value) ? endSelectStart.value : ends[0]
  }
}
function matchesSeatFilter(seat) {
  if (seatFilter.value === 'all') return true
  if (seatFilter.value === 'power') return !!seat.has_power
  if (seatFilter.value === 'window') return !!seat.near_window
  if (seatFilter.value === 'quiet') return !!seat.quiet_zone
  if (seatFilter.value === 'hot') return !!seat.hot_seat
  return true
}
function seatVisualClass(seat) {
  const classes = []
  if (selectedSeat.value?.id === seat.id) classes.push('sel')
  else if (!seat.is_seat || !seat.available) classes.push('busy')
  else classes.push('free')
  if (!matchesSeatFilter(seat) && seat.is_seat) classes.push('off')
  return classes
}
function canSelectSeat(seat) {
  return !!seat.is_seat && !!seat.available && matchesSeatFilter(seat)
}
function applyQuickSlot(slot) {
  if (slot.expired || isQuickSlotExpired(slot)) {
    notify('该时段已开始超过15分钟，无法预约')
    return
  }
  reservationForm.startTime = slot.start
  reservationForm.endTime = slot.end
  loadAvailableSeats()
}
function isQuickSlotActive(slot) {
  return reservationForm.startTime === slot.start && reservationForm.endTime === slot.end
}
function openConfirmReservation() {
  if (!selectedSeat.value) return
  confirmReservationOpen.value = true
}
function toggleNotifyPref(key) {
  notifyPrefs[key] = !notifyPrefs[key]
  localStorage.setItem('notifyPrefs', JSON.stringify(notifyPrefs))
}
function goBackStudent() {
  const profilePages = ['myres', 'credit', 'stats', 'settings', 'feedback', 'notifications']
  studentPage.value = profilePages.includes(studentPage.value) && studentPage.value !== 'notifications' ? 'profile' : 'home'
}
function selectSeatDirect(seat) {
  if (!canSelectSeat(seat)) return
  selectedSeat.value = seat
  confirmReservationOpen.value = true
}
function openFeedbackModal() {
  feedbackForm.content = ''
  feedbackForm.type = 'SUGGESTION'
  feedbackForm.severity = 'MEDIUM'
  feedbackModalOpen.value = true
}
function openProfileInfo() {
  syncProfileForm()
  profileInfoOpen.value = true
}
function confirmCheckout() {
  if (!activeReservation.value) return
  openModalConfirm('确认签退', '签退后将结束本次学习并释放座位，确定签退吗？', doCheckout)
}
async function doCheckout() {
  try {
    const data = await call('post', `/reservations/${activeReservation.value.id}/checkout`)
    checkoutSummary.value = {
      roomName: activeReservation.value.roomName,
      seatNo: activeReservation.value.seatNo,
      minutes: data.actualMinutes || studySeconds.value / 60,
      creditChange: '+5'
    }
    checkoutModalOpen.value = true
    await loadReservations()
    await loadCredit()
  } catch (e) { notify(e.message) }
}
function openModalConfirm(title, message, onOk) {
  genericModal.title = title
  genericModal.message = message
  genericModal.onConfirm = onOk
  genericModal.open = true
}
function clearSession(message) {
  token.value = ''
  role.value = ''
  me.value = {}
  studentPage.value = 'home'
  adminPage.value = 'checkins'
  loginRole.value = 'student'
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  if (message) notify(message)
}
function openRegister() {
  registerPassword2.value = ''
  registerOpen.value = true
}
async function onRegisterFile(event) {
  const file = event.target.files?.[0]
  if (!file) {
    registerForm.materialUrl = ''
    return
  }
  try {
    const form = new FormData()
    form.append('file', file)
    const res = await api.post('/auth/register/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    registerForm.materialUrl = res.data.data.url
    notify('身份材料上传成功')
  } catch (e) {
    notify(e.message || '材料上传失败')
  }
}
async function call(method, url, data, config) {
  const res = await api.request({ method, url, data, ...config })
  return res.data.data
}
async function loginStudent() {
  if (authLoading.value) return
  if (!studentLogin.username.trim() || !studentLogin.password) {
    notify('请输入学号和密码')
    return
  }
  authLoading.value = true
  try {
    const data = await call('post', '/auth/login', studentLogin)
    afterLogin(data.token, 'STUDENT', data.userInfo)
  } catch (e) {
    notify(e.message || '登录失败')
  } finally {
    authLoading.value = false
  }
}
async function loginAdmin() {
  if (authLoading.value) return
  if (!adminLogin.account.trim() || !adminLogin.password) {
    notify('请输入管理员账号和密码')
    return
  }
  authLoading.value = true
  try {
    const data = await call('post', '/admin/auth/login', adminLogin)
    afterLogin(data.token, data.adminInfo.role === 'SUPER_ADMIN' ? 'SUPER_ADMIN' : 'ADMIN', data.adminInfo)
  } catch (e) {
    notify(e.message || '登录失败')
  } finally {
    authLoading.value = false
  }
}
async function afterLogin(t, r, info) {
  token.value = t
  role.value = r
  me.value = info || {}
  localStorage.setItem('token', t)
  localStorage.setItem('role', r)
  notify('登录成功')
  await bootstrap(false)
}
async function register() {
  if (authLoading.value) return
  if (!registerForm.studentNo.trim() || !registerForm.name.trim() || !registerForm.password) {
    notify('请填写学号、姓名和密码')
    return
  }
  if (registerForm.studentNo.trim().length < 10) {
    notify('学号格式不正确（至少 10 位）')
    return
  }
  if (!registerForm.phone.trim() || !/^1\d{10}$/.test(registerForm.phone.trim())) {
    notify('请输入 11 位中国大陆手机号')
    return
  }
  if (!registerForm.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email.trim())) {
    notify('邮箱格式不正确')
    return
  }
  if (!registerForm.materialUrl) {
    notify('请上传身份材料')
    return
  }
  if (registerForm.password !== registerPassword2.value) {
    notify('两次密码输入不一致')
    return
  }
  if (registerForm.password.length < 6 || registerForm.password.length > 20) {
    notify('密码长度须为 6-20 位')
    return
  }
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(registerForm.password)) {
    notify('密码须同时包含字母和数字')
    return
  }
  authLoading.value = true
  try {
    await call('post', '/auth/register', { ...registerForm })
    registerOpen.value = false
    registerPassword2.value = ''
    notify('注册申请已提交，请等待管理员审核')
  } catch (e) {
    notify(e.message || '注册失败')
  } finally {
    authLoading.value = false
  }
}
function logout() {
  clearSession('已退出登录')
}
/** @param silent 页面刷新时用旧 token 恢复会话：失败则静默清 token，避免登录页弹 SQL 报错 */
async function bootstrap(silent = true) {
  if (!token.value) return
  try {
    me.value = await call('get', '/auth/me')
    syncProfileForm()
    await Promise.all([loadRooms(), loadAnnouncements()])
    if (role.value === 'STUDENT') {
      await Promise.all([loadReservations(), loadNotifications(), loadCredit(), loadStudyStats()])
    } else {
      await openAdmin('checkins')
    }
  } catch (e) {
    clearSession('')
    if (!silent) {
      notify(e.message || '登录后加载失败，请重试')
    }
  }
}
async function loadRooms() {
  rooms.value = await call('get', role.value === 'STUDENT' ? '/rooms' : '/admin/rooms')
  if (!reservationForm.roomId && rooms.value[0]) reservationForm.roomId = rooms.value[0].id
  await loadAvailableSeats()
}
async function loadAvailableSeats() {
  if (!reservationForm.roomId) return
  normalizeReservationTimes()
  ensureEndAfterStart()
  seats.value = await call('get', '/seats/available', null, { params: { roomId: reservationForm.roomId, date: reservationForm.date, startTime: reservationForm.startTime, endTime: reservationForm.endTime } })
  selectedSeat.value = null
}
async function handleRoomChange() {
  normalizeReservationTimes()
  clampReservationStartForToday()
  await loadAvailableSeats()
}
async function setReservationDate(date) {
  reservationForm.date = date
  clampReservationStartForToday()
  await loadAvailableSeats()
}
function selectRoom(id) {
  reservationForm.roomId = id
  studentPage.value = 'reservation'
  handleRoomChange()
}
async function handleStartTimeChange() {
  normalizeReservationTimes()
  ensureEndAfterStart()
  clampReservationStartForToday()
  await loadAvailableSeats()
}
async function handleEndTimeChange() {
  normalizeReservationTimes()
  if (timeToMinutes(reservationForm.endTime) <= timeToMinutes(reservationForm.startTime)) {
    reservationForm.endTime = endSelectStart.value
    notify('结束时间必须晚于开始时间')
  }
  await loadAvailableSeats()
}
function openSeatDetail(seat) {
  selectSeatDirect(seat)
}
function confirmSeatSelection() {
  if (!pendingSeat.value?.available) return
  selectedSeat.value = pendingSeat.value
  seatDialogOpen.value = false
}
function seatUnavailableText(seat) {
  if (!seat?.is_seat || seat?.reserveState === 'disabled' || seat?.status !== 'NORMAL') return '不可预约'
  if (seat?.reserveState === 'reserved') return '当前时段已被预约'
  return '不可预约'
}
async function createReservation() {
  try {
    ensureEndAfterStart()
    ensureReservationTimeAllowed()
    await call('post', '/reservations', { roomId: reservationForm.roomId, seatId: selectedSeat.value.id, reserveDate: reservationForm.date, startTime: reservationForm.startTime, endTime: reservationForm.endTime })
    confirmReservationOpen.value = false
    notify('预约成功')
    await Promise.all([loadReservations(), loadAvailableSeats()])
    studentPage.value = 'checkin'
  } catch (e) { notify(e.message) }
}
async function loadReservations() {
  reservations.value = await call('get', '/reservations/my')
}
async function cancelReservation(r) {
  await call('post', `/reservations/${r.id}/cancel`)
          notify('已取消预约，扣除 50 信用分')
  await Promise.all([loadReservations(), loadCredit()])
}
async function checkout() {
  confirmCheckout()
}
async function startTempLeave() {
  try {
    await call('post', `/reservations/${activeReservation.value.id}/temp-leave`)
    notify('已开始暂离，请按时返回')
    await loadReservations()
  } catch (e) { notify(e.message) }
}
async function endTempLeave() {
  try {
    await call('post', `/reservations/${activeReservation.value.id}/temp-return`)
    notify('已返回座位')
    await loadReservations()
  } catch (e) { notify(e.message) }
}
async function saveProfile() {
  try {
    me.value = await call('put', '/student/profile', profileForm)
    notify('资料已保存')
  } catch (e) { notify(e.message) }
}
async function uploadLayoutImage(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!token.value) {
    notify('请先登录管理端后再上传图片')
    return
  }
  try {
    const form = new FormData()
    form.append('file', file)
    form.append('category', 'layout')
    const res = await api.post('/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    roomForm.layoutImageUrl = res.data.data.url
    notify('图片上传成功')
  } catch (err) { notify(err.message || '上传失败') }
}
function syncProfileForm() {
  Object.assign(profileForm, {
    name: me.value.name || '',
    phone: me.value.phone || '',
    email: me.value.email || '',
    college: me.value.college || '',
    major: me.value.major || '',
    grade: me.value.grade || ''
  })
}
async function loadCredit() {
  credit.value = await call('get', '/credits/my')
}
async function loadStudyStats() {
  studyStats.value = await call('get', '/statistics/my-study-duration', null, { params: { period: statPeriod.value } })
}
async function loadAnnouncements() {
  announcements.value = await call('get', '/announcements')
}
async function readAnnouncement(a) {
  await call('post', `/announcements/${a.id}/read`)
  activeAnnouncement.value = a
  announcementDetailOpen.value = true
  await loadAnnouncements()
}
async function loadNotifications() {
  notifications.value = await call('get', '/notifications')
}
function openNotifications() {
  studentPage.value = 'notifications'
  loadNotifications()
}
async function readNotification(n) {
  await call('post', `/notifications/${n.id}/read`)
  await loadNotifications()
}
async function readAllNotifications() {
  await call('post', '/notifications/read-all')
  await loadNotifications()
}
async function submitFeedback(payload) {
  const content = typeof payload === 'string' ? payload : payload?.content
  const severity = typeof payload === 'object' && payload?.severity ? payload.severity : 'MEDIUM'
  if (!String(content || '').trim()) return
  await call('post', '/feedback', { content, type: 'SUGGESTION', severity, roomId: reservationForm.roomId, seatId: selectedSeat.value?.id })
  notify('反馈已提交')
  studentPage.value = 'profile'
}
async function changeStatPeriod(period) {
  statPeriod.value = period
  await loadStudyStats()
}
function statusText(status) {
  return { PENDING: '待使用', USING: '使用中', TEMP_LEAVE: '暂离中', COMPLETED: '已完成', CANCELLED: '已取消', VIOLATED: '违约', AUTO_CANCELLED: '超时取消', AUTO_CHECKOUT: '自动签退' }[status] || status
}
async function openAdmin(page) {
  adminPage.value = page
  if (page === 'users') await loadUsers()
  if (page === 'admins') await loadAdminAccounts()
  if (page === 'rooms') {
    await loadRooms()
    if (isSuperAdmin.value) await loadAdminAccounts()
  }
  if (page === 'reservations') adminReservations.value = await call('get', '/admin/reservations')
  if (page === 'checkins') {
    checkins.value = await call('get', '/admin/checkins')
    scanStudentNo.value = ''
    scanHint.value = ''
    await loadLiveReservations()
  }
  if (page === 'announcements') await loadAnnouncements()
  if (page === 'statistics') {
    await loadRooms()
    await loadAdminStatistics()
  }
  if (page === 'feedback') adminFeedback.value = await call('get', '/admin/feedback')
  if (page === 'settings') {
    operationLogs.value = await call('get', '/admin/operation-logs')
    if (isSuperAdmin.value) await loadAdminAccounts()
  }
}
async function loadAdminAccounts() {
  try {
    adminAccounts.value = await call('get', '/admin/admins')
  } catch (e) {
    adminAccounts.value = []
    if (isSuperAdmin.value) notify(e.message)
  }
}
function openAdminForm(row = null) {
  if (!isSuperAdmin.value) return notify('仅超级管理员可管理管理员账号')
  Object.assign(adminForm, {
    id: row?.id || null,
    account: row?.account || '',
    name: row?.name || '',
    phone: row?.phone || '',
    password: '',
    isSuperAdmin: row?.role === 'SUPER_ADMIN'
  })
  adminFormOpen.value = true
}
async function saveAdminAccount() {
  if (!adminForm.account.trim() || !adminForm.name.trim()) return notify('请填写账号与姓名')
  if (!adminForm.id && (!adminForm.password || adminForm.password.length < 6)) return notify('请设置至少6位初始密码')
  try {
    if (adminForm.id) {
      await call('put', `/admin/admins/${adminForm.id}`, {
        name: adminForm.name.trim(),
        phone: adminForm.phone.trim(),
        password: adminForm.password || undefined
      })
    } else {
      await call('post', '/admin/admins', {
        account: adminForm.account.trim(),
        name: adminForm.name.trim(),
        phone: adminForm.phone.trim(),
        password: adminForm.password
      })
    }
    adminFormOpen.value = false
    notify('管理员已保存')
    await loadAdminAccounts()
  } catch (e) { notify(e.message) }
}
async function disableAdminAccount(row) {
  try {
    await call('post', `/admin/admins/${row.id}/disable`)
    notify('已禁用')
    await loadAdminAccounts()
  } catch (e) { notify(e.message) }
}
async function enableAdminAccount(row) {
  try {
    await call('post', `/admin/admins/${row.id}/enable`)
    notify('已启用')
    await loadAdminAccounts()
  } catch (e) { notify(e.message) }
}
function resolveUserId(rowOrId) {
  if (rowOrId && typeof rowOrId === 'object') {
    return rowOrId.userId ?? rowOrId.user_id ?? rowOrId.id
  }
  return rowOrId
}
function auditStatusLabel(status) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status || '-'
}
function accountStatusLabel(status) {
  return { NORMAL: '正常', PENDING: '待审核', DISABLED: '已禁用', BLACKLIST: '黑名单' }[status] || status || '-'
}
function decorateUserRow(row) {
  return {
    ...row,
    accountStatus: row.accountStatus || row.status,
    auditLabel: auditStatusLabel(row.audit_status),
    statusLabel: accountStatusLabel(row.accountStatus || row.status)
  }
}
async function loadUsers() {
  const params = { keyword: userKeyword.value || undefined }
  if (userAuditFilter.value) params.auditStatus = userAuditFilter.value
  users.value = (await call('get', '/admin/users', null, { params })).map(decorateUserRow)
  userPage.value = 1
}
function exportUsersCsv() {
  api.get('/admin/users/export', {
    responseType: 'blob',
    params: {
      keyword: userKeyword.value || undefined,
      auditStatus: userAuditFilter.value || undefined
    }
  }).then(res => {
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'student-users.csv'
    a.click()
    URL.revokeObjectURL(url)
    notify('用户 CSV 已导出')
  }).catch(e => notify(e.message || '导出失败'))
}
function openUserDetail(row) {
  userDetail.value = { ...row }
  userDetailOpen.value = true
}
function approveFromDetail() {
  approve(userDetail.value).then(() => { userDetailOpen.value = false })
}
function rejectFromDetail() {
  reject(userDetail.value)
  userDetailOpen.value = false
}
async function approve(row) {
  const id = resolveUserId(row)
  if (!id) return notify('无法识别用户 ID')
  try {
    await call('post', `/admin/users/${id}/approve`, {})
    notify('审核通过')
    await loadUsers()
  } catch (e) { notify(e.message) }
}
function reject(row) {
  const id = resolveUserId(row)
  if (!id) return notify('无法识别用户 ID')
  rejectUserId.value = id
  rejectRemark.value = ''
  rejectOpen.value = true
}
function openChangePassword() {
  changePasswordForm.oldPassword = ''
  changePasswordForm.newPassword = ''
  changePasswordForm.confirmPassword = ''
  changePasswordOpen.value = true
}
async function submitChangePassword() {
  if (!changePasswordForm.oldPassword || !changePasswordForm.newPassword || !changePasswordForm.confirmPassword) {
    return notify('请填写完整密码信息')
  }
  if (changePasswordForm.newPassword !== changePasswordForm.confirmPassword) {
    return notify('两次新密码不一致')
  }
  if (changePasswordForm.newPassword.length < 6 || changePasswordForm.newPassword.length > 20) {
    return notify('新密码长度需为 6-20 位')
  }
  if (!/(?=.*[A-Za-z])(?=.*\d)/.test(changePasswordForm.newPassword)) {
    return notify('新密码需同时包含字母和数字')
  }
  try {
    await call('post', '/auth/change-password', {
      oldPassword: changePasswordForm.oldPassword,
      newPassword: changePasswordForm.newPassword
    })
    changePasswordOpen.value = false
    notify('密码已修改，请使用新密码重新登录')
    logout()
  } catch (e) { notify(e.message) }
}
async function confirmReject() {
  if (!rejectUserId.value) return
  try {
    await call('post', `/admin/users/${rejectUserId.value}/reject`, { remark: rejectRemark.value || '资料不符合要求' })
    rejectOpen.value = false
    notify('已拒绝')
    await loadUsers()
  } catch (e) { notify(e.message) }
}
async function disable(row) {
  const id = resolveUserId(row)
  if (!id) return notify('无法识别用户 ID')
  try {
    await call('post', `/admin/users/${id}/disable`)
    notify('已禁用')
    await loadUsers()
  } catch (e) { notify(e.message) }
}
async function enable(row) {
  const id = resolveUserId(row)
  if (!id) return notify('无法识别用户 ID')
  try {
    await call('post', `/admin/users/${id}/enable`)
    notify('已启用')
    await loadUsers()
  } catch (e) { notify(e.message) }
}
function editRoom(r = {}) {
  Object.assign(roomForm, {
    id: r.id || null,
    roomCode: r.room_code || '',
    name: r.name || '',
    location: r.location || '',
    floor: r.floor || '1楼',
    openTime: String(r.open_time || '07:00:00'),
    closeTime: String(r.close_time || '22:30:00'),
    layoutImageUrl: r.layout_image_url || '',
    rowCount: r.row_count || 4,
    colCount: r.col_count || 6,
    status: r.status || 'OPEN',
    facilities: r.facilities || '空调,WiFi',
    managerId: r.manager_id || r.managerId || null
  })
  seatKeyword.value = ''
  seatStatusFilter.value = ''
  roomFormOpen.value = true
  if (r.id) {
    nextTick(() => loadAdminSeats())
  } else {
    adminSeats.value = []
  }
}
async function saveRoom() {
  if (!roomForm.name?.trim()) return notify('请填写自习室名称')
  if (!roomForm.location?.trim()) return notify('请填写自习室位置')
  if (isSuperAdmin.value && !roomForm.id && !roomForm.managerId) return notify('请选择自习室负责人')
  const method = roomForm.id ? 'put' : 'post'
  const url = roomForm.id ? `/admin/rooms/${roomForm.id}` : '/admin/rooms'
  try {
    const payload = {
      roomCode: roomForm.roomCode || `ROOM-${Date.now()}`,
      name: roomForm.name.trim(),
      location: roomForm.location.trim(),
      floor: roomForm.floor || '1楼',
      openTime: roomForm.openTime || '07:00:00',
      closeTime: roomForm.closeTime || '22:30:00',
      facilities: roomForm.facilities || '空调,WiFi',
      layoutImageUrl: roomForm.layoutImageUrl || '',
      rowCount: roomForm.rowCount || 4,
      colCount: roomForm.colCount || 6,
      status: roomForm.status || 'OPEN'
    }
    if (isSuperAdmin.value && roomForm.managerId) payload.managerId = roomForm.managerId
    const isCreate = !roomForm.id
    const saved = await call(method, url, payload)
    if (saved?.id) roomForm.id = saved.id
    notify(isCreate ? '自习室已创建，可在下方编辑座位' : '自习室已保存，座位网格已同步，可继续在下方编辑座位')
    await loadRooms()
    await loadAdminSeats()
    if (reservationForm.roomId && Number(reservationForm.roomId) === Number(roomForm.id)) {
      await loadAvailableSeats()
    }
  } catch (e) { notify(e.message || '保存失败') }
}
async function deleteRoom(r) {
  try {
    await call('delete', `/admin/rooms/${r.id}`)
    notify('已删除')
    await loadRooms()
  } catch (e) { notify(e.message) }
}
async function loadAdminSeats() {
  if (!roomFormOpen.value || !roomForm.id) {
    adminSeats.value = []
    return
  }
  adminSeats.value = await call('get', `/admin/rooms/${roomForm.id}/seats`)
}
async function toggleSeat(s) {
  await call('put', `/admin/seats/${s.id}`, { ...s, isSeat: s.is_seat, cellCategory: s.cell_category, seatType: s.seat_type, hasPower: s.has_power, nearWindow: s.near_window, quietZone: s.quiet_zone, hotSeat: s.hot_seat, status: s.status === 'NORMAL' ? 'DISABLED' : 'NORMAL' })
  await loadAdminSeats()
}
async function scanCheckin() {
  if (scanBusy.value) return
  const studentNo = scanStudentNo.value.trim()
  if (!studentNo) {
    notify('请输入学生学号')
    return
  }
  scanBusy.value = true
  scanHint.value = `正在提交签到（学号 ${studentNo}）…`
  try {
    await call('post', '/admin/checkin/scan', { studentNo })
    notify('签到成功')
    scanStudentNo.value = ''
    scanHint.value = '签到成功。可继续输入学号或拍照扫码下一位学生。'
    try {
      checkins.value = await call('get', '/admin/checkins')
      await loadLiveReservations()
    } catch {
      /* 列表刷新失败不影响签到结果 */
    }
  } catch (e) {
    scanHint.value = e.message || '签到失败，请重试或检查网络'
    notify(e.message || '签到失败')
  } finally {
    scanBusy.value = false
  }
}
/** 从拍照/二维码文本解析学号（支持纯学号或旧版 token 二维码） */
function normalizeStudentNoFromScan(raw) {
  const text = String(raw || '').trim()
  if (/^\d{10,20}$/.test(text)) return text
  try {
    let b64 = text.replace(/-/g, '+').replace(/_/g, '/')
    while (b64.length % 4) b64 += '='
    const decoded = atob(b64)
    const parts = decoded.split(':')
    if (parts.length >= 4 && /^\d{10,20}$/.test(parts[3])) return parts[3]
  } catch { /* 非 token */ }
  return ''
}
function triggerPhotoScan() {
  if (scanBusy.value) return
  scanPhotoInput.value?.click()
}
function loadImageFromUrl(url) {
  return new Promise((resolve, reject) => {
    const el = new Image()
    el.onload = () => resolve(el)
    el.onerror = () => reject(new Error('无法读取照片'))
    el.src = url
  })
}
function buildScanCanvas(img, maxSide) {
  let w = img.naturalWidth || img.width
  let h = img.naturalHeight || img.height
  if (!w || !h) return null
  if (Math.max(w, h) > maxSide) {
    const ratio = maxSide / Math.max(w, h)
    w = Math.max(1, Math.round(w * ratio))
    h = Math.max(1, Math.round(h * ratio))
  }
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  if (!ctx) return null
  ctx.drawImage(img, 0, 0, w, h)
  return canvas
}
function decodeJsQrFromCanvas(canvas) {
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  if (!ctx) return ''
  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height)
  const result = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'attemptBoth' })
  return result?.data?.trim() || ''
}
async function tryBarcodeDetectorOnCanvas(canvas) {
  if (!('BarcodeDetector' in window)) return ''
  try {
    const detector = new window.BarcodeDetector({ formats: ['qr_code'] })
    const codes = await Promise.race([
      detector.detect(canvas),
      new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 1200))
    ])
    return codes[0]?.rawValue?.trim() || ''
  } catch {
    return ''
  }
}
async function decodeQrFromImageFile(file) {
  const name = file.name || ''
  const type = file.type || ''
  if (/heic|heif/i.test(type) || /\.heic$|\.heif$/i.test(name)) {
    throw new Error('HEIC 照片浏览器无法解析，请点「拍照」现拍 JPG，或直接输入学号')
  }
  const url = URL.createObjectURL(file)
  try {
    const img = await loadImageFromUrl(url)
    const sizes = [960, 1280, 640]
    for (const size of sizes) {
      const canvas = buildScanCanvas(img, size)
      if (!canvas) continue
      const fromJs = decodeJsQrFromCanvas(canvas)
      if (fromJs) return fromJs
    }
    const fallbackCanvas = buildScanCanvas(img, 960)
    if (fallbackCanvas) {
      const fromNative = await tryBarcodeDetectorOnCanvas(fallbackCanvas)
      if (fromNative) return fromNative
    }
    return ''
  } finally {
    URL.revokeObjectURL(url)
  }
}
async function onScanPhotoSelected(ev) {
  const file = ev.target?.files?.[0]
  if (ev.target) ev.target.value = ''
  if (!file || scanBusy.value) return
  scanBusy.value = true
  scanHint.value = '正在识别照片（已自动压缩，请稍候）…'
  try {
    const raw = await decodeQrFromImageFile(file)
    if (!raw) {
      scanHint.value = '未识别到二维码（与手机好坏无关，常因拍屏摩尔纹/相册 HEIC）。请直接输入学号，或让学生把二维码放大、斜 30° 再拍。'
      notify(scanHint.value)
      return
    }
    const studentNo = normalizeStudentNoFromScan(raw)
    if (!/^\d{10,20}$/.test(studentNo)) {
      scanHint.value = '识别内容不是有效学号，请让学生出示签到页的学号二维码，或手动输入学号。'
      notify(scanHint.value)
      return
    }
    scanStudentNo.value = studentNo
    scanHint.value = `已识别学号 ${studentNo}，正在提交签到…`
    await call('post', '/admin/checkin/scan', { studentNo })
    notify('签到成功')
    scanStudentNo.value = ''
    scanHint.value = '签到成功。可继续输入学号或拍照扫码下一位学生。'
    try {
      checkins.value = await call('get', '/admin/checkins')
      await loadLiveReservations()
    } catch { /* ignore */ }
  } catch (e) {
    const msg = e.message || '照片解析或签到失败'
    scanHint.value = msg.includes('timeout') ? '请求超时，请确认与电脑同一 WiFi 后重试' : msg
    notify(scanHint.value)
  } finally {
    scanBusy.value = false
  }
}
async function refreshCheckinQr() {
  const no = studentNoDisplay.value
  if (!no || no === '—') {
    checkinQrSvg.value = ''
    return
  }
  try {
    checkinQrSvg.value = await createQrSvg(no)
  } catch {
    checkinQrSvg.value = ''
  }
}
function editAnnouncement(a = {}) {
  Object.assign(announcementForm, { id: a.id, title: a.title || '', content: a.content || '', type: a.type || 'SYSTEM', pinned: !!a.pinned })
  announcementDialog.value = true
}
async function saveAnnouncement() {
  const payload = { ...announcementForm, pinned: announcementForm.pinned ? 1 : 0, status: 'PUBLISHED' }
  await call(announcementForm.id ? 'put' : 'post', announcementForm.id ? `/admin/announcements/${announcementForm.id}` : '/admin/announcements', payload)
  announcementDialog.value = false
  notify('公告已保存')
  await loadAnnouncements()
}
function openFeedbackHandle(row) {
  feedbackHandleForm.id = row.id
  feedbackHandleForm.studentName = row.studentName
  feedbackHandleForm.type = row.type
  feedbackHandleForm.content = row.content
  feedbackHandleForm.handleResult = ''
  feedbackHandleOpen.value = true
}
async function submitFeedbackHandle() {
  if (!feedbackHandleForm.handleResult?.trim()) return notify('请填写处理说明')
  try {
    await call('put', `/admin/feedback/${feedbackHandleForm.id}`, {
      status: 'DONE',
      handleResult: feedbackHandleForm.handleResult.trim()
    })
    feedbackHandleOpen.value = false
    notify('反馈已处理，已通知学生')
    adminFeedback.value = await call('get', '/admin/feedback')
  } catch (e) { notify(e.message) }
}
function openRevokeViolation(row) {
  revokeViolationForm.id = row._rawId || row.id
  revokeViolationForm.studentName = row.studentName || '—'
  revokeViolationForm.reservationNo = row.reservation_no || '—'
  revokeViolationForm.roomName = row.roomName || '—'
  revokeViolationForm.seatNo = row.seatNo || '—'
  revokeViolationForm.reserveDate = formatDate(row.reserve_date || row.reserveDate)
  revokeViolationForm.remark = ''
  revokeViolationOpen.value = true
}
async function submitRevokeViolation() {
  if (!revokeViolationForm.id) return
  try {
    await call('post', `/admin/reservations/${revokeViolationForm.id}/revoke-violation`, {
      remark: revokeViolationForm.remark?.trim() || ''
    })
    revokeViolationOpen.value = false
    notify('违约已撤销，信用分已恢复')
    adminReservations.value = await call('get', '/admin/reservations')
  } catch (e) { notify(e.message) }
}
async function addAdminSeat() {
  if (!roomForm.id) return notify('请先保存自习室')
  try {
    await call('post', `/admin/rooms/${roomForm.id}/seats`, {})
    notify('座位已补全')
    await loadAdminSeats()
    await loadRooms()
  } catch (e) { notify(e.message) }
}
async function deleteSeatEdit() {
  if (!seatEditForm.id) return
  const seatId = seatEditForm.id
  const seatLabel = seatEditForm.seat_no || '该座位'
  seatEditOpen.value = false
  await nextTick()
  openModalConfirm('删除座位', `确定删除座位 ${seatLabel} 吗？删除后不可恢复。`, async () => {
    try {
      await call('delete', `/admin/seats/${seatId}`)
      notify('座位已删除')
      await loadAdminSeats()
      await loadRooms()
    } catch (e) { notify(e.message) }
  })
}
async function loadAdminStatistics() {
  try {
    const params = { period: adminStatsPeriod.value, rangeMode: adminStatsRangeMode.value }
    if (adminStatsRoomId.value) params.roomId = adminStatsRoomId.value
    adminStatsReport.value = await call('get', '/admin/statistics/report', null, { params })
    await nextTick()
    drawUsageChart()
  } catch (e) { notify(e.message) }
}
function downloadReport() {
  const params = { period: adminStatsPeriod.value, rangeMode: adminStatsRangeMode.value }
  if (adminStatsRoomId.value) params.roomId = adminStatsRoomId.value
  api.get('/admin/statistics/export', { responseType: 'blob', params }).then(res => {
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = 'study-room-report.csv'
    a.click()
    URL.revokeObjectURL(url)
  })
}
function drawStudentChart() {
  nextTick(() => {
    const el = studentChart.value
    if (!el) return
    echarts.init(el).setOption({
      tooltip: hourTooltip(),
      xAxis: { type: 'category', name: '日期', data: (studyStats.value.series || []).map(x => String(x.label).slice(5, 10)) },
      yAxis: hourYAxis(),
      series: [{ name: '学习时长', type: 'bar', data: (studyStats.value.series || []).map(x => Number(((Number(x.minutes || 0)) / 60).toFixed(1))), itemStyle: { color: '#5f73fb' } }]
    })
  })
}
function drawUsageChart() {
  if (!usageChart.value) return
  const chart = echarts.init(usageChart.value)
  const periodLabel = adminStatsReport.value.summary?.periodLabel || '今日'
  if (statAdminView.value === 'peak') {
    const data = adminStatsReport.value.peak || []
    chart.setOption({
      tooltip: countTooltip(),
      title: { text: `${periodLabel}高峰时段`, left: 'center', textStyle: { fontSize: 14 } },
      xAxis: { type: 'category', name: '时段', data: data.map(x => peakAxisLabel(x)) },
      yAxis: countYAxis('预约次数（次）'),
      series: [{ name: '预约数', type: 'bar', data: data.map(x => statCount(x)), itemStyle: { color: '#4f6ef7' } }]
    }, true)
    return
  }
  const data = adminStatsReport.value.usage || []
  if (statAdminView.value === 'share') {
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}<br/>预约 {c} 次（{d}%）' },
      title: { text: `${periodLabel}自习室预约占比`, left: 'center', textStyle: { fontSize: 14 } },
      series: [{ name: '预约占比', type: 'pie', radius: '70%', data: data.map(x => ({ name: x.roomName, value: x.reservationCount || x.usageRate })) }]
    }, true)
    return
  }
  const trend = adminStatsReport.value.trend || []
  if (trend.length) {
    chart.setOption({
      tooltip: countTooltip(),
      title: { text: `${periodLabel}预约趋势`, left: 'center', textStyle: { fontSize: 14 } },
      xAxis: { type: 'category', name: '时段', data: trend.map(x => trendAxisLabel(x)) },
      yAxis: countYAxis(),
      series: [{ name: '预约数', type: 'line', smooth: true, data: trend.map(x => statCount(x)), itemStyle: { color: '#6c5ce7' } }]
    }, true)
    return
  }
  chart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v) => `${v}%` },
    title: { text: `${periodLabel}各自习室使用率`, left: 'center', textStyle: { fontSize: 14 } },
    xAxis: { type: 'category', name: '自习室', data: data.map(x => x.roomName) },
    yAxis: percentYAxis(),
    series: [{ name: '使用率', type: 'bar', data: data.map(x => x.usageRate), itemStyle: { color: '#6c5ce7' } }]
  }, true)
}
async function switchStatAdminView(view) {
  statAdminView.value = view
  await nextTick()
  drawUsageChart()
}

const ReservationCard = defineComponent({
  props: { item: Object, statusText: Function },
  emits: ['cancel'],
  setup(props, { emit }) {
    return () => h('article', { class: 'card reservation-card' }, [
      h('div', [
        h('strong', props.item.roomName || props.item.room),
        h('p', `${props.item.reserve_date || props.item.date} · ${String(props.item.start_time || '').slice(0, 5)}-${String(props.item.end_time || '').slice(0, 5)}`),
        h('span', { class: `reservation-status ${props.item.status || ''}` }, props.statusText ? props.statusText(props.item.status) : props.item.status)
      ]),
      h('span', { class: 'seat-left' }, props.item.seatNo || props.item.seat),
      props.item.status === 'PENDING' ? h('button', { class: 'mini-btn', onClick: () => emit('cancel') }, '取消') : null
    ])
  }
})
const FeedbackBox = defineComponent({
  emits: ['submit'],
  setup(_, { emit }) {
    const content = ref('')
    const severity = ref('MEDIUM')
    const severityOptions = [
      { value: 'LOW', label: '低' },
      { value: 'MEDIUM', label: '中' },
      { value: 'HIGH', label: '高' },
      { value: 'CRITICAL', label: '紧急' }
    ]
    return () => h('div', { class: 'card feedback-box' }, [
      h('strong', '问题反馈'),
      h('label', { class: 'feedback-severity-label' }, '严重程度'),
      h('select', {
        class: 'input',
        value: severity.value,
        onChange: e => { severity.value = e.target.value }
      }, severityOptions.map(opt => h('option', { value: opt.value }, `${opt.label} — ${opt.value === 'LOW' ? '一般建议' : opt.value === 'MEDIUM' ? '影响使用' : opt.value === 'HIGH' ? '较严重' : '需立即处理'}`))),
      h('textarea', { placeholder: '描述遇到的问题或建议', value: content.value, onInput: e => { content.value = e.target.value } }),
      h('button', {
        class: 'primary-action small',
        onClick: () => {
          if (content.value.trim()) {
            emit('submit', { content: content.value, severity: severity.value })
            content.value = ''
            severity.value = 'MEDIUM'
          }
        }
      }, '提交反馈')
    ])
  }
})
const DataTable = defineComponent({
  props: { rows: Array, columns: Array, columnLabels: { type: Object, default: () => ADMIN_COLUMN_LABELS }, emptyText: { type: String, default: '暂无数据' } },
  setup(props, { slots }) {
    const label = c => props.columnLabels[c] || c
    const cell = (c, row) => formatAdminCell(c, row[c], row)
    return () => h('div', { class: 'table-wrap' }, [
      !(props.rows || []).length ? h('p', { class: 'muted table-empty' }, props.emptyText) : null,
      h('table', [
        h('thead', h('tr', [...props.columns.map(c => h('th', label(c))), slots.actions ? h('th', '操作') : null].filter(Boolean))),
        h('tbody', (props.rows || []).map(row => h('tr', [
          ...props.columns.map(c => h('td', cell(c, row))),
          slots.actions ? h('td', slots.actions({ row })) : null
        ].filter(Boolean))))
      ])
    ])
  }
})
const AdminPager = defineComponent({
  name: 'AdminPager',
  props: {
    page: { type: Number, required: true },
    total: { type: Number, required: true },
    count: { type: Number, default: 0 }
  },
  emits: ['update:page'],
  setup(props, { emit }) {
    return () => {
      if (!props.count) return null
      const nodes = []
      if (props.total > 1) {
        nodes.push(h('div', { class: 'admin-pager' },
          Array.from({ length: props.total }, (_, i) => i + 1).map(p =>
            h('button', {
              type: 'button',
              class: { active: props.page === p },
              onClick: () => emit('update:page', p)
            }, String(p))
          )
        ))
      }
      nodes.push(h('p', { class: 'admin-pager-meta scanner-hint' }, `共 ${props.count} 条 · 第 ${props.page}/${props.total} 页`))
      return h('div', { class: 'admin-pager-wrap' }, nodes)
    }
  }
})

window.addEventListener('resize', () => { width.value = window.innerWidth })
onMounted(() => {
  try {
    const saved = JSON.parse(localStorage.getItem('notifyPrefs') || '{}')
    Object.assign(notifyPrefs, saved)
  } catch (e) { /* ignore */ }
  studyTimerHandle = setInterval(updateStudyTimer, 1000)
  bootstrap()
})
watch(studentPage, (page) => {
  if (page === 'checkin') {
    startCheckinPagePoll()
    refreshCheckinQr()
  } else {
    stopCheckinPagePoll()
    checkinQrSvg.value = ''
  }
}, { immediate: true })
watch([activeReservation, studentNoDisplay], () => {
  if (studentPage.value === 'checkin' && activeReservation.value?.status === 'PENDING') {
    refreshCheckinQr()
  } else if (activeReservation.value?.status !== 'PENDING') {
    checkinQrSvg.value = ''
  }
})
watch([adminKeyword, adminStatusFilter], () => { adminAccountPage.value = 1 })
watch([reservationKeyword, reservationStatusFilter, reservationRoomFilter], () => { reservationPage.value = 1 })
watch([checkinKeyword, checkinResultFilter], () => { checkinPage.value = 1 })
watch(decoratedLiveReservations, () => { liveReservationPage.value = 1 })
watch([feedbackKeyword, feedbackStatusFilter], () => { feedbackPage.value = 1 })
watch([logKeyword, logModuleFilter], () => { logPage.value = 1 })
watch([roomKeyword, roomStatusFilter], () => { roomPage.value = 1 })
watch(announcementKeyword, () => { announcementPage.value = 1 })
watch(users, () => { userPage.value = 1 })
onBeforeUnmount(() => {
  stopCheckinPagePoll()
  if (studyTimerHandle) clearInterval(studyTimerHandle)
})
</script>
