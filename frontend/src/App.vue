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
          <div class="time-slots">
            <button v-for="slot in quickTimeSlots" :key="slot.label" type="button" class="time-chip" :class="{ active: isQuickSlotActive(slot) }" @click="applyQuickSlot(slot)">{{ slot.label }}</button>
          </div>
          <div class="card reserve-config">
            <div class="time-select-row">
              <label>开始</label>
              <el-time-select v-model="reservationForm.startTime" start="07:00" step="00:10" end="22:20" @change="handleStartTimeChange" />
              <span>→</span>
              <label>结束</label>
              <el-time-select v-model="reservationForm.endTime" :start="endSelectStart" step="00:10" end="22:30" @change="handleEndTimeChange" />
            </div>
          </div>

          <h2 class="section-title">🧮 预约配置</h2>
          <div class="card reserve-config">
            <label>自习室</label>
            <el-select v-model="reservationForm.roomId" @change="loadAvailableSeats">
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
              <div class="check-actions-state">
                <button type="button" class="round-action primary-round" :disabled="!canOpenCheckinQr" @click="loadQr">
                  <span>📱</span><strong>出示二维码签到</strong>
                </button>
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
              <span>/ 300</span>
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
            <div class="credit-rule"><strong>按时签到</strong> +10 分；超时未签到 -50 分；暂离超时 -30 分。</div>
            <div class="credit-rule"><strong>信用等级</strong>：280 分以上优秀，200 分以上良好，低于 200 需改进。</div>
          </div>
          <div class="timeline">
            <div v-for="l in credit.logs" :key="l.id" class="timeline-item">
              <strong>{{ l.change_value > 0 ? '+' : '' }}{{ l.change_value }}</strong>
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
                <strong>{{ b.value }}</strong>
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
          <template v-if="adminPage === 'dashboard'">
            <div class="admin-dashboard-grid">
              <div class="stat-card"><div class="lbl">今日预约</div><div class="num">{{ dashboard.todayReservations ?? 0 }}</div></div>
              <div class="stat-card"><div class="lbl">活跃用户</div><div class="num">{{ dashboard.activeUsers ?? 0 }}</div></div>
              <div class="stat-card"><div class="lbl">座位使用率</div><div class="num">{{ dashboard.seatUsageRate ?? 0 }}%</div></div>
              <div class="stat-card"><div class="lbl">今日违约</div><div class="num">{{ dashboard.violationToday ?? 0 }}</div></div>
            </div>
            <div class="card">
              <h3 class="section-title">本周预约趋势</h3>
              <div ref="adminChart" class="chart"></div>
            </div>
            <h3 class="section-title">实时预约</h3>
            <DataTable :rows="decoratedLiveReservations" :columns="['studentNo','studentName','roomName','seatNo','reserveDate','status']" />
          </template>

          <template v-if="adminPage === 'users'">
            <el-input v-model="userKeyword" placeholder="搜索学号或姓名" @input="loadUsers" />
            <div class="filter-row user-audit-filters">
              <button v-for="f in userAuditFilters" :key="f.key" type="button" :class="{ active: userAuditFilter === f.key }" @click="userAuditFilter = f.key; loadUsers()">{{ f.label }}</button>
            </div>
            <DataTable :rows="pagedUsers" :columns="['student_no','name','college','credit_score','auditLabel','statusLabel']" empty-text="暂无用户数据">
              <template #actions="{ row }">
                <el-button v-if="row.audit_status === 'PENDING'" size="small" type="success" @click="approve(row)">通过</el-button>
                <el-button v-if="row.audit_status === 'PENDING'" size="small" type="warning" @click="reject(row)">拒绝</el-button>
                <el-button v-if="row.accountStatus !== 'DISABLED' && row.audit_status === 'APPROVED'" size="small" @click="disable(row)">禁用</el-button>
                <el-button v-if="row.accountStatus === 'DISABLED'" size="small" @click="enable(row)">启用</el-button>
              </template>
            </DataTable>
            <div class="admin-pager">
              <button v-for="p in userTotalPages" :key="p" type="button" :class="{ active: userPage === p }" @click="userPage = p">{{ p }}</button>
            </div>
          </template>

          <template v-if="adminPage === 'rooms'">
            <div class="admin-head-actions">
              <h3>自习室管理</h3>
              <button type="button" class="btn btn-primary" @click="toggleRoomForm">{{ roomFormExpanded ? '收起表单' : '新增自习室' }}</button>
            </div>
            <div v-if="roomFormExpanded" class="room-form-panel">
              <h3>{{ roomForm.id ? '编辑自习室' : '新增自习室' }}</h3>
              <div class="room-row">
                <div class="field"><label>编号</label><input v-model="roomForm.roomCode" class="input" placeholder="编号" /></div>
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
              <button type="button" class="btn btn-primary btn-block" @click="saveRoom">保存自习室</button>
            </div>
            <article class="room-item" v-for="r in rooms" :key="r.id">
              <div>
                <div class="room-item-head"><strong>{{ r.name }}</strong><span class="mini-badge active">余 {{ r.availableSeats ?? r.available_seats ?? 0 }}</span></div>
                <p class="muted">{{ r.location }} · {{ r.floor || '未设置' }} · {{ roomStatusText(r.status) }}</p>
                <div class="room-tags">
                  <span v-for="tag in parseRoomFacilities(r)" :key="tag" class="room-tag">{{ tag }}</span>
                </div>
              </div>
              <div>
                <button type="button" class="btn btn-outline" @click="editRoom(r)">编辑</button>
                <button type="button" class="btn btn-danger" @click="deleteRoom(r)">删除</button>
              </div>
            </article>
          </template>

          <template v-if="adminPage === 'seats'">
            <div class="admin-head-actions">
              <el-select v-model="adminSeatRoomId" placeholder="选择自习室" @change="loadAdminSeats" style="min-width:220px">
                <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
              </el-select>
              <button type="button" class="btn btn-primary" :disabled="!adminSeatRoomId" @click="addAdminSeat">新增座位</button>
            </div>
            <p class="scanner-hint">点击格子可编辑属性；删除前请确认无进行中预约。</p>
            <div class="seat-map-grid" :style="{ gridTemplateColumns: `repeat(${adminRoom?.col_count || 6}, minmax(0, 1fr))` }">
              <button
                v-for="(s, idx) in adminSeats"
                :key="s.id"
                type="button"
                class="cell-grid-btn"
                :class="seatCellClass(s)"
                @click="openSeatEdit(s)"
              >
                <div>R{{ s.row_no || Math.floor(idx / (adminRoom?.col_count || 6)) + 1 }}-C{{ s.col_no || (idx % (adminRoom?.col_count || 6)) + 1 }}</div>
                <div class="cell-tags">
                  <span v-for="tag in seatCellTags(s)" :key="tag" class="cell-tag">{{ tag }}</span>
                </div>
              </button>
            </div>
          </template>

          <template v-if="adminPage === 'reservations'">
            <DataTable :rows="decoratedAdminReservations" :columns="['reservation_no','studentName','roomName','seatNo','reserve_date','status']" empty-text="暂无预约记录" />
          </template>

          <template v-if="adminPage === 'checkins'">
            <div class="card scan-box">
              <div class="scanner-toolbar">
                <button v-if="canUseLiveCamera" type="button" class="btn btn-primary" @click="toggleCameraScan">
                  {{ isScanning ? '停止扫码' : '实时扫码' }}
                </button>
                <button type="button" class="btn btn-primary" @click="triggerPhotoScan">拍照扫码</button>
                <button type="button" class="btn btn-outline" :disabled="!scanToken.trim()" @click="scanCheckin">手动确认</button>
                <input ref="scanPhotoInput" type="file" accept="image/*" capture="environment" class="scan-photo-input" @change="onScanPhotoSelected" />
              </div>
              <div class="scanner-view" v-show="isScanning">
                <video ref="scanVideo" autoplay muted playsinline playsInline webkit-playsinline></video>
                <div class="scanner-frame"></div>
              </div>
              <p class="scanner-hint">{{ scanHint || defaultScanHint }}</p>
              <p v-if="cameraPermissionHint" class="scanner-permission-hint">{{ cameraPermissionHint }}</p>
              <input v-model="scanToken" class="input" placeholder="摄像头无法使用时，可粘贴学生签到二维码 token" />
            </div>
            <DataTable :rows="decoratedCheckins" :columns="['studentName','roomName','seatNo','checkin_time','checkout_time','result']" empty-text="暂无签到记录" />
          </template>

          <template v-if="adminPage === 'announcements'">
            <el-button type="primary" @click="editAnnouncement()">发布公告</el-button>
            <article class="card announcement" v-for="a in announcements" :key="a.id">
              <strong>{{ a.title }}</strong><p>{{ a.content }}</p>
              <el-button size="small" @click="editAnnouncement(a)">编辑</el-button>
            </article>
          </template>

          <template v-if="adminPage === 'statistics'">
            <div class="admin-head-actions">
              <h3>统计分析</h3>
              <button type="button" class="btn btn-primary" @click="downloadReport">导出报表</button>
            </div>
            <div class="period-tabs adminStatsPeriod">
              <button type="button" :class="{ active: adminStatsPeriod === 'day' }" @click="changeAdminStatsPeriod('day')">日报</button>
              <button type="button" :class="{ active: adminStatsPeriod === 'week' }" @click="changeAdminStatsPeriod('week')">周报</button>
              <button type="button" :class="{ active: adminStatsPeriod === 'month' }" @click="changeAdminStatsPeriod('month')">月报</button>
            </div>
            <div class="stat-view-tabs">
              <button type="button" :class="{ active: statAdminView === 'usage' }" @click="switchStatAdminView('usage')">使用统计</button>
              <button type="button" :class="{ active: statAdminView === 'peak' }" @click="switchStatAdminView('peak')">高峰分析</button>
              <button type="button" :class="{ active: statAdminView === 'share' }" @click="switchStatAdminView('share')">自习室占比</button>
            </div>
            <p class="scanner-hint">当前统计周期：{{ adminStatsReport.summary?.periodLabel || '今日' }}</p>
            <div class="admin-dashboard-grid">
              <div class="stat-card"><div class="lbl">总预约</div><div class="num">{{ adminStatSummary.totalReserve }}</div></div>
              <div class="stat-card"><div class="lbl">使用中</div><div class="num">{{ adminStatSummary.usingCount }}</div></div>
              <div class="stat-card"><div class="lbl">签到率</div><div class="num">{{ adminStatSummary.checkinRate }}%</div></div>
              <div class="stat-card"><div class="lbl">平均信用分</div><div class="num">{{ adminStatSummary.avgCredit }}</div></div>
            </div>
            <div class="card"><div ref="usageChart" class="chart"></div></div>
          </template>

          <template v-if="adminPage === 'feedback'">
            <DataTable :rows="decoratedAdminFeedback" :columns="['studentName','roomName','seatNo','type','severity','content','status']" empty-text="暂无反馈">
              <template #actions="{ row }">
                <el-button v-if="row._rawStatus === 'PENDING' || row._rawStatus === 'PROCESSING'" size="small" type="primary" @click="openFeedbackHandle(row)">标记处理</el-button>
                <span v-else class="muted">已处理</span>
              </template>
            </DataTable>
          </template>

          <template v-if="adminPage === 'settings'">
            <div class="card">当前管理员：{{ me.name }} · {{ me.role }}</div>
            <h3>最近操作日志</h3>
            <DataTable :rows="operationLogs" :columns="['module','action','target_type','detail','created_at']" empty-text="暂无操作日志" />
          </template>
        </main>
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

    <div v-if="qrModalOpen" class="modal-mask" @click.self="closeQrModal">
      <div class="modal-card" style="max-width:320px">
        <div class="modal-head">
          <div class="modal-title">{{ qrCheckinSuccess ? '签到成功' : '请向管理员出示二维码' }}</div>
          <button type="button" class="modal-close" @click="closeQrModal">✕</button>
        </div>
        <div class="qr-box" style="text-align:center">
          <div v-if="qrCheckinSuccess" class="qr-success-state">
            <div class="qr-success-icon">✓</div>
            <p class="qr-success-text">签到成功</p>
            <small class="muted">5 秒后自动关闭</small>
          </div>
          <template v-else>
            <div class="qr-image" v-html="qrSvg"></div>
            <div class="qr-meta">
              <strong>{{ qrInfo.name }} · {{ qrInfo.studentNo }}</strong>
              <span>{{ qrInfo.roomName }} · {{ qrInfo.seatNo }}</span>
              <small>{{ qrCountdown > 0 ? `${qrCountdown} 秒后过期` : '正在刷新…' }}</small>
            </div>
          </template>
        </div>
        <div v-if="!qrCheckinSuccess" class="modal-actions">
          <button type="button" class="btn btn-outline" @click="loadQr">刷新二维码</button>
          <button type="button" class="btn btn-outline" @click="copyQrToken">复制 token</button>
          <button type="button" class="btn btn-primary" @click="closeQrModal">关闭</button>
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

    <div v-if="genericModal.open" class="modal-mask" @click.self="genericModal.open = false">
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

    <div v-if="seatEditOpen" class="modal-mask" @click.self="seatEditOpen = false">
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

const api = axios.create({ baseURL: '/api' })
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
const qrToken = ref('')
const qrSvg = ref('')
const qrCountdown = ref(0)
const qrCheckinSuccess = ref(false)
let qrCountdownTimer = null
let qrSuccessCloseTimer = null
let checkinPollTimer = null
const qrInfo = ref({})
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
  { key: 'quiet', label: '静音区' },
  { key: 'hot', label: '热门' }
]
const quickTimeSlots = [
  { label: '08:00-10:00', start: '08:00', end: '10:00' },
  { label: '10:00-12:00', start: '10:00', end: '12:00' },
  { label: '14:00-16:00', start: '14:00', end: '16:00' },
  { label: '19:00-21:00', start: '19:00', end: '21:00' }
]
const confirmReservationOpen = ref(false)
const qrModalOpen = ref(false)
const checkoutModalOpen = ref(false)
const checkoutSummary = ref({})
const profileInfoOpen = ref(false)
const feedbackModalOpen = ref(false)
const feedbackForm = reactive({ type: 'SUGGESTION', severity: 'MEDIUM', content: '' })
const roomFormExpanded = ref(false)
const seatEditOpen = ref(false)
const seatEditForm = reactive({})
const seatEditEnabled = computed({
  get: () => seatEditForm.status === 'NORMAL',
  set: val => { seatEditForm.status = val ? 'NORMAL' : 'DISABLED' }
})
const adminStatsPeriod = ref('week')
const userPage = ref(1)
const userPageSize = 3
const adminAccounts = ref([])
const rejectRemark = ref('')
const rejectUserId = ref(null)
const rejectOpen = ref(false)
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
const peakStats = ref([])
const aboutOpen = ref(false)
const notifyPrefs = reactive({ reservation: true, checkin: true, announcement: true, dnd: false })
let studyTimerHandle = null

const adminPage = ref('dashboard')
const dashboard = ref({})
const users = ref([])
const userKeyword = ref('')
const adminReservations = ref([])
const checkins = ref([])
const adminFeedback = ref([])
const adminStatsReport = ref({ summary: {}, usage: [], peak: [], trend: [], credit: [] })
const feedbackHandleOpen = ref(false)
const feedbackHandleForm = reactive({ id: null, studentName: '', type: '', content: '', handleResult: '' })
const adminSeatRoomId = ref(null)
const adminSeats = ref([])
const scanToken = ref('')
const scanVideo = ref(null)
const scanPhotoInput = ref(null)
const isScanning = ref(false)
const scanHint = ref('')
const cameraPermissionHint = ref('')
let scanStream = null
let scanFrame = 0
let barcodeDetector = null
const adminChart = ref(null)
const usageChart = ref(null)
const roomForm = reactive({})
const announcementDialog = ref(false)
const announcementForm = reactive({})
const profileForm = reactive({ name: '', phone: '', email: '', college: '', major: '', grade: '' })
const operationLogs = ref([])
const tempLeaveHint = ref('暂离中，请在 30 分钟内返回座位')

const todayText = computed(() => new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' }))
/** 实时摄像头仅 HTTPS 或 localhost 可用；局域网 IP + HTTP 需用「拍照扫码」 */
const canUseLiveCamera = computed(() => {
  if (typeof window === 'undefined') return true
  return window.isSecureContext || location.hostname === 'localhost' || location.hostname === '127.0.0.1'
})
const defaultScanHint = computed(() => canUseLiveCamera.value
  ? '电脑端可点「实时扫码」打开摄像头；手机局域网访问请用「拍照扫码」。'
  : '当前为局域网 HTTP，请点「拍照扫码」对准学生二维码拍照，系统会自动识别并完成签到。')
const homeDateText = computed(() => `${new Date().getMonth() + 1}月${new Date().getDate()}日`)
const unreadCount = computed(() => notifications.value.filter(n => !n.read_flag).length)
const currentRoom = computed(() => rooms.value.find(r => r.id === reservationForm.roomId))
const adminRoom = computed(() => rooms.value.find(r => r.id === adminSeatRoomId.value))
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
const canOpenCheckinQr = computed(() => {
  const r = activeReservation.value
  return !!r && r.status === 'PENDING' && isWithinCheckinWindow(r)
})
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
  const order = ['精品区', '热门区', '静音区', '静音区-电源位', '标准区', '非座位区']
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
const dashboardUsageRate = computed(() => {
  const rows = dashboard.value.usage || []
  if (!rows.length) return 0
  const sum = rows.reduce((acc, r) => acc + Number(r.usageRate || 0), 0)
  return (sum / rows.length).toFixed(1)
})
const studentTitle = computed(() => ({ home: '首页', reservation: '座位预约', checkin: '签到签退', profile: '我的', myres: '我的预约', credit: '信用积分', stats: '学习统计', notifications: '消息通知', settings: '设置', feedback: '问题反馈' }[studentPage.value] || '首页'))
const shownReservations = computed(() => reservationStatus.value === 'ALL' ? reservations.value : reservations.value.filter(r => r.status === reservationStatus.value))
const availableSeatCount = computed(() => seats.value.filter(s => s.available).length)
const roomLayoutImage = computed(() => currentRoom.value?.layout_image_url || currentRoom.value?.layoutImageUrl || '')
const endSelectStart = computed(() => addMinutes(reservationForm.startTime, 10))
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
const adminNav = [
  { page: 'dashboard', label: '仪表盘', icon: '📊' },
  { page: 'users', label: '用户管理', icon: '👥' },
  { page: 'rooms', label: '自习室', icon: '🏫' },
  { page: 'seats', label: '座位', icon: '🪑' },
  { page: 'reservations', label: '预约', icon: '📅' },
  { page: 'checkins', label: '签到', icon: '✅' },
  { page: 'announcements', label: '公告', icon: '📣' },
  { page: 'statistics', label: '统计', icon: '📈' },
  { page: 'feedback', label: '反馈', icon: '💬' },
  { page: 'settings', label: '设置', icon: '⚙️' }
]
const sortedAnnouncements = computed(() => [...announcements.value].sort((a, b) => Number(b.pinned || 0) - Number(a.pinned || 0)))
const pagedUsers = computed(() => {
  const start = (userPage.value - 1) * userPageSize
  return users.value.slice(start, start + userPageSize)
})
const userTotalPages = computed(() => Math.max(1, Math.ceil(users.value.length / userPageSize)))
const decoratedLiveReservations = computed(() =>
  (dashboard.value.liveReservations || []).map(r => ({
    ...r,
    reserveDate: formatDate(r.reserveDate || r.reserve_date),
    status: reservationStatusText(r.status)
  }))
)
const decoratedAdminReservations = computed(() => adminReservations.value.map(decorateReservationRow))
const decoratedCheckins = computed(() => checkins.value.map(decorateCheckinRow))
const decoratedAdminFeedback = computed(() => adminFeedback.value.map(row => {
  const d = decorateFeedbackRow(row)
  return { ...d, _rawStatus: row.status }
}))
const adminStatSummary = computed(() => {
  const s = adminStatsReport.value.summary || {}
  if (s.totalReserve != null) {
    return {
      totalReserve: s.totalReserve || 0,
      usingCount: s.usingCount || 0,
      checkinRate: s.checkinRate || 0,
      avgCredit: s.avgCredit || 0
    }
  }
  const usage = dashboard.value.usage || []
  const totalReserve = usage.reduce((acc, row) => acc + Number(row.reservationCount || 0), 0)
  const usedCount = usage.reduce((acc, row) => acc + Number(row.usedCount || 0), 0)
  const checkinRate = totalReserve ? Math.round((usedCount / totalReserve) * 100) : 0
  return { totalReserve, usingCount: dashboard.value.usingCount || 0, checkinRate, avgCredit: 0 }
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
function roomStatusText(status) {
  return { OPEN: '开放', CLOSED: '关闭', MAINTENANCE: '维护中' }[status] || status || '-'
}
function clearQrCountdown() {
  if (qrCountdownTimer) {
    clearInterval(qrCountdownTimer)
    qrCountdownTimer = null
  }
}
function clearQrSuccessCloseTimer() {
  if (qrSuccessCloseTimer) {
    clearTimeout(qrSuccessCloseTimer)
    qrSuccessCloseTimer = null
  }
}
function stopCheckinPagePoll() {
  if (checkinPollTimer) {
    clearInterval(checkinPollTimer)
    checkinPollTimer = null
  }
}
function closeQrModal() {
  qrModalOpen.value = false
  qrCheckinSuccess.value = false
  clearQrCountdown()
  clearQrSuccessCloseTimer()
}
async function showQrCheckinSuccess() {
  if (qrCheckinSuccess.value) return
  qrCheckinSuccess.value = true
  clearQrCountdown()
  try { await loadReservations() } catch { /* ignore */ }
  notify('签到成功')
  updateStudyTimer()
  clearQrSuccessCloseTimer()
  qrSuccessCloseTimer = setTimeout(() => closeQrModal(), 5000)
}
/** 签到页统一轮询：二维码成功反馈、自动切到学习中、无效签到被撤销后回退 */
async function syncCheckinStateFromServer() {
  if (studentPage.value !== 'checkin') return
  const prev = activeReservation.value
  const prevStatus = prev?.status
  const prevId = prev?.id
  try {
    await loadReservations()
    const cur = activeReservation.value
    if (!cur) return
    if (qrModalOpen.value && !qrCheckinSuccess.value) {
      const targetId = qrInfo.value?.reservationId
      if (Number(cur.id) === Number(targetId) && ['USING', 'TEMP_LEAVE'].includes(cur.status)) {
        await showQrCheckinSuccess()
        return
      }
    }
    if (prevStatus === 'PENDING' && cur.status === 'USING' && Number(prevId) === Number(cur.id)) {
      updateStudyTimer()
      if (!qrModalOpen.value) notify('签到成功，已进入学习状态')
    }
    if (prevStatus === 'USING' && cur.status === 'PENDING' && Number(prevId) === Number(cur.id)) {
      notify('签到无效，已恢复为待签到')
      closeQrModal()
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
function toggleRoomForm() {
  if (roomFormExpanded.value && !roomForm.id) {
    roomFormExpanded.value = false
    return
  }
  editRoom()
}
function openSeatEdit(seat) {
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
function resolveSeatSection(seat) {
  if (!seat.is_seat) return '非座位区'
  if (seat.quiet_zone && seat.has_power) return '静音区-电源位'
  if (seat.quiet_zone) return '静音区'
  if (seat.has_power || seat.near_window) return '精品区'
  if (seat.hot_seat) return '热门区'
  return '标准区'
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
  stopCameraScan()
  clearQrCountdown()
  token.value = ''
  role.value = ''
  me.value = {}
  studentPage.value = 'home'
  adminPage.value = 'dashboard'
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
      await openAdmin('dashboard')
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
  if (!adminSeatRoomId.value && rooms.value[0]) adminSeatRoomId.value = rooms.value[0].id
  await loadAvailableSeats()
}
async function loadAvailableSeats() {
  if (!reservationForm.roomId) return
  ensureEndAfterStart()
  seats.value = await call('get', '/seats/available', null, { params: { roomId: reservationForm.roomId, date: reservationForm.date, startTime: reservationForm.startTime, endTime: reservationForm.endTime } })
  selectedSeat.value = null
}
async function setReservationDate(date) {
  reservationForm.date = date
  await loadAvailableSeats()
}
function selectRoom(id) {
  reservationForm.roomId = id
  studentPage.value = 'reservation'
  loadAvailableSeats()
}
async function handleStartTimeChange() {
  ensureEndAfterStart()
  await loadAvailableSeats()
}
async function handleEndTimeChange() {
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
  notify('已取消预约')
  await loadReservations()
}
async function loadQr() {
  if (!canOpenCheckinQr.value) {
    notify(checkinWindowHint.value || '当前不在签到时间内')
    return
  }
  try {
    const data = await call('get', '/checkin/qrcode')
    qrToken.value = data.qrToken
    qrInfo.value = data
    qrCheckinSuccess.value = false
    qrSvg.value = await createQrSvg(data.qrToken)
    qrModalOpen.value = true
    clearQrCountdown()
    qrCountdown.value = data.expireSeconds || 60
    qrCountdownTimer = setInterval(() => {
      qrCountdown.value -= 1
      if (qrCountdown.value <= 0) {
        if (!canOpenCheckinQr.value) {
          notify('签到时间已结束，请关闭后重新预约')
          closeQrModal()
          return
        }
        loadQr().catch((e) => {
          notify(e.message || '二维码已过期，请重新生成')
          closeQrModal()
        })
      }
    }, 1000)
  } catch (e) { notify(e.message) }
}
async function copyQrToken() {
  if (!qrToken.value) return notify('请先生成二维码')
  try {
    await navigator.clipboard.writeText(qrToken.value)
    notify('token 已复制，可粘贴到管理端「手动确认」')
  } catch {
    notify('复制失败，请长按选中 token 手动复制')
  }
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
async function submitFeedback(content) {
  await call('post', '/feedback', { content, type: 'SUGGESTION', severity: 'MEDIUM', roomId: reservationForm.roomId, seatId: selectedSeat.value?.id })
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
  if (page !== 'checkins') stopCameraScan()
  adminPage.value = page
  if (page === 'dashboard') {
    dashboard.value = await call('get', '/admin/dashboard')
    peakStats.value = dashboard.value.peak || []
    await nextTick()
    drawAdminChart()
  }
  if (page === 'users') await loadUsers()
  if (page === 'rooms') await loadRooms()
  if (page === 'seats') await loadAdminSeats()
  if (page === 'reservations') adminReservations.value = await call('get', '/admin/reservations')
  if (page === 'checkins') {
    checkins.value = await call('get', '/admin/checkins')
    scanHint.value = '点击「扫码签到」后，浏览器会弹出摄像头权限申请；若未弹出，请查看地址栏左侧的相机图标。'
  }
  if (page === 'announcements') await loadAnnouncements()
  if (page === 'statistics') await loadAdminStatistics()
  if (page === 'feedback') adminFeedback.value = await call('get', '/admin/feedback')
  if (page === 'settings') {
    operationLogs.value = await call('get', '/admin/operation-logs')
    if (role.value === 'SUPER_ADMIN') adminAccounts.value = await call('get', '/admin/admins')
  }
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
    facilities: r.facilities || '空调,WiFi'
  })
  roomFormExpanded.value = true
}
async function saveRoom() {
  if (!roomForm.name?.trim()) return notify('请填写自习室名称')
  if (!roomForm.location?.trim()) return notify('请填写自习室位置')
  const method = roomForm.id ? 'put' : 'post'
  const url = roomForm.id ? `/admin/rooms/${roomForm.id}` : '/admin/rooms'
  try {
    await call(method, url, {
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
    })
    roomFormExpanded.value = false
    notify('自习室已保存')
    await loadRooms()
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
  if (!adminSeatRoomId.value) return
  adminSeats.value = await call('get', `/admin/rooms/${adminSeatRoomId.value}/seats`)
}
async function toggleSeat(s) {
  await call('put', `/admin/seats/${s.id}`, { ...s, isSeat: s.is_seat, cellCategory: s.cell_category, seatType: s.seat_type, hasPower: s.has_power, nearWindow: s.near_window, quietZone: s.quiet_zone, hotSeat: s.hot_seat, status: s.status === 'NORMAL' ? 'DISABLED' : 'NORMAL' })
  await loadAdminSeats()
}
async function scanCheckin() {
  if (!scanToken.value.trim()) {
    notify('请先扫描或粘贴学生签到 token')
    return
  }
  try {
    await call('post', '/admin/checkin/scan', { qrToken: scanToken.value.trim() })
    notify('签到成功')
    scanToken.value = ''
    scanHint.value = ''
    checkins.value = await call('get', '/admin/checkins')
  } catch (e) { notify(e.message) }
}
function triggerPhotoScan() {
  scanPhotoInput.value?.click()
}
async function decodeQrFromImageFile(file) {
  const url = URL.createObjectURL(file)
  try {
    const img = await new Promise((resolve, reject) => {
      const el = new Image()
      el.onload = () => resolve(el)
      el.onerror = () => reject(new Error('无法读取照片'))
      el.src = url
    })
    const scales = [1, 1.5, 0.75, 0.5, 2, 2.5]
    for (const scale of scales) {
      const token = decodeQrFromImageElement(img, scale)
      if (token) return token
    }
    return ''
  } finally {
    URL.revokeObjectURL(url)
  }
}
function decodeQrFromImageElement(img, scale = 1) {
  const maxSide = 1600
  let w = Math.round(img.naturalWidth * scale)
  let h = Math.round(img.naturalHeight * scale)
  if (Math.max(w, h) > maxSide) {
    const ratio = maxSide / Math.max(w, h)
    w = Math.round(w * ratio)
    h = Math.round(h * ratio)
  }
  w = Math.max(1, w)
  h = Math.max(1, h)
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d', { willReadFrequently: true })
  ctx.drawImage(img, 0, 0, w, h)
  const imageData = ctx.getImageData(0, 0, w, h)
  const result = jsQR(imageData.data, imageData.width, imageData.height, { inversionAttempts: 'attemptBoth' })
  return result?.data?.trim() || ''
}
async function onScanPhotoSelected(ev) {
  const file = ev.target?.files?.[0]
  if (ev.target) ev.target.value = ''
  if (!file) return
  scanHint.value = '正在识别照片中的二维码…'
  try {
    const token = await decodeQrFromImageFile(file)
    if (!token) {
      scanHint.value = '未识别到二维码，请重新拍照，确保二维码完整、光线充足。'
      notify(scanHint.value)
      return
    }
    scanToken.value = token
    await scanCheckin()
    scanHint.value = canUseLiveCamera.value
      ? '签到成功。可继续实时扫码或拍照扫码。'
      : '签到成功。请继续为下一位学生「拍照扫码」。'
  } catch (e) {
    scanHint.value = '照片解析失败，请重试。'
    notify(e.message || scanHint.value)
  }
}
function toggleCameraScan() {
  if (isScanning.value) {
    stopCameraScan()
    return
  }
  startCameraScan()
}
function cameraErrorMessage(err) {
  const name = err?.name || ''
  if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
    return '摄像头权限被拒绝。请点击浏览器地址栏左侧的锁/相机图标 → 允许摄像头，然后再次点击「扫码签到」。'
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
    return '未检测到可用摄像头。请连接摄像头，或改用手动粘贴 token。'
  }
  if (name === 'NotReadableError' || name === 'TrackStartError') {
    return '摄像头可能被其他软件占用（如微信、Zoom）。请关闭后重试。'
  }
  if (name === 'OverconstrainedError') {
    return '当前摄像头不支持所需参数，请改用手动粘贴 token。'
  }
  if (!window.isSecureContext) {
    return '局域网 HTTP 无法实时打开摄像头（浏览器安全限制）。请使用「拍照扫码」。'
  }
  const msg = err?.message ? String(err.message) : '未知错误'
  return `摄像头启动失败：${msg}。请改用手动粘贴 token。`
}
async function openCameraStream() {
  const attempts = [
    { audio: false, video: { width: { ideal: 1280 }, height: { ideal: 720 } } },
    { audio: false, video: { facingMode: 'user' } },
    { audio: false, video: true }
  ]
  let lastError = null
  for (const constraints of attempts) {
    try {
      return await navigator.mediaDevices.getUserMedia(constraints)
    } catch (e) {
      lastError = e
      if (e?.name === 'NotAllowedError' || e?.name === 'PermissionDeniedError') {
        throw e
      }
    }
  }
  throw lastError || new Error('无法打开摄像头')
}
async function startCameraScan() {
  cameraPermissionHint.value = ''
  if (!window.isSecureContext && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
    scanHint.value = cameraErrorMessage({ name: 'InsecureContext' })
    notify(scanHint.value)
    triggerPhotoScan()
    return
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    scanHint.value = '当前浏览器不支持摄像头 API，请使用 Chrome / Edge，或改用手动粘贴 token。'
    notify(scanHint.value)
    return
  }
  try {
    isScanning.value = true
    scanHint.value = '正在请求摄像头权限，请在浏览器弹窗中选择「允许」…'
    await nextTick()
    scanStream = await openCameraStream()
    if (!scanVideo.value) {
      await nextTick()
    }
    if (!scanVideo.value) {
      throw new Error('视频组件未就绪')
    }
    scanVideo.value.srcObject = scanStream
    scanVideo.value.setAttribute('playsinline', 'true')
    scanVideo.value.setAttribute('webkit-playsinline', 'true')
    await scanVideo.value.play()
    scanHint.value = '摄像头已打开。将学生签到二维码对准框内；若不能自动识别，可复制 token 到下方输入框。'
    cameraPermissionHint.value = ''

    if (!('BarcodeDetector' in window)) {
      scanHint.value = '摄像头已打开。当前浏览器不支持自动识别二维码，请让学生出示二维码后，使用 Chrome 浏览器或改用手动粘贴 token。'
      notify('摄像头已就绪，当前浏览器需手动粘贴 token')
      return
    }
    const supportedFormats = await window.BarcodeDetector.getSupportedFormats?.()
    if (supportedFormats && !supportedFormats.includes('qr_code')) {
      scanHint.value = '摄像头已打开，但无法自动识别 QR 码，请改用手动粘贴 token。'
      return
    }
    barcodeDetector = new window.BarcodeDetector({ formats: ['qr_code'] })
    scanCameraFrame()
  } catch (e) {
    stopCameraScan(false)
    scanHint.value = cameraErrorMessage(e)
    if (e?.name === 'NotAllowedError' || e?.name === 'PermissionDeniedError') {
      cameraPermissionHint.value = '提示：Edge/Chrome 地址栏左侧点击 🔒 或 🎥 → 站点权限 → 摄像头 → 允许，然后重新点击「扫码签到」。'
    }
    notify(scanHint.value)
  }
}
async function scanCameraFrame() {
  if (!isScanning.value || !scanVideo.value) return
  if (barcodeDetector) {
    try {
      const codes = await barcodeDetector.detect(scanVideo.value)
      if (codes.length > 0) {
        scanToken.value = codes[0].rawValue
        stopCameraScan()
        await scanCheckin()
        return
      }
    } catch (e) {
      scanHint.value = '正在识别二维码，请保持画面清晰稳定。'
    }
  }
  scanFrame = requestAnimationFrame(scanCameraFrame)
}
function stopCameraScan(resetHint = true) {
  isScanning.value = false
  if (scanFrame) {
    cancelAnimationFrame(scanFrame)
    scanFrame = 0
  }
  if (scanStream) {
    scanStream.getTracks().forEach(track => track.stop())
    scanStream = null
  }
  if (scanVideo.value) scanVideo.value.srcObject = null
  barcodeDetector = null
  if (resetHint) {
    scanHint.value = ''
    cameraPermissionHint.value = ''
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
async function addAdminSeat() {
  if (!adminSeatRoomId.value) return notify('请先选择自习室')
  try {
    await call('post', `/admin/rooms/${adminSeatRoomId.value}/seats`, {})
    notify('已新增座位')
    await loadAdminSeats()
    await loadRooms()
  } catch (e) { notify(e.message) }
}
async function deleteSeatEdit() {
  if (!seatEditForm.id) return
  openModalConfirm('删除座位', '删除后不可恢复，确定删除该座位吗？', async () => {
    try {
      await call('delete', `/admin/seats/${seatEditForm.id}`)
      seatEditOpen.value = false
      notify('座位已删除')
      await loadAdminSeats()
      await loadRooms()
    } catch (e) { notify(e.message) }
  })
}
async function loadAdminStatistics() {
  try {
    adminStatsReport.value = await call('get', '/admin/statistics/report', null, { params: { period: adminStatsPeriod.value } })
    await nextTick()
    drawUsageChart()
  } catch (e) { notify(e.message) }
}
function downloadReport() {
  api.get('/admin/statistics/export', { responseType: 'blob', params: { period: adminStatsPeriod.value } }).then(res => {
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
    echarts.init(el).setOption({ xAxis: { type: 'category', data: (studyStats.value.series || []).map(x => String(x.label).slice(5, 10)) }, yAxis: { type: 'value' }, series: [{ type: 'bar', data: (studyStats.value.series || []).map(x => x.minutes), itemStyle: { color: '#5f73fb' } }] })
  })
}
function drawAdminChart() {
  const trend = dashboard.value.weeklyTrend || []
  if (adminChart.value) {
    echarts.init(adminChart.value).setOption({
      tooltip: {},
      title: { text: '本周预约趋势', left: 'center', textStyle: { fontSize: 14 } },
      xAxis: { type: 'category', data: trend.map(x => x.label) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: trend.map(x => x.count), itemStyle: { color: '#6c5ce7' } }]
    })
  }
}
function drawUsageChart() {
  if (!usageChart.value) return
  const chart = echarts.init(usageChart.value)
  const periodLabel = adminStatsReport.value.summary?.periodLabel || '今日'
  if (statAdminView.value === 'peak') {
    const data = adminStatsReport.value.peak?.length ? adminStatsReport.value.peak : (peakStats.value || [])
    chart.setOption({
      tooltip: {},
      title: { text: `${periodLabel}高峰时段`, left: 'center', textStyle: { fontSize: 14 } },
      xAxis: { type: 'category', data: data.map(x => `${x.hour}时`) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: data.map(x => x.count), itemStyle: { color: '#4f6ef7' } }]
    }, true)
    return
  }
  const data = adminStatsReport.value.usage?.length ? adminStatsReport.value.usage : (dashboard.value.usage || [])
  if (statAdminView.value === 'share') {
    chart.setOption({
      tooltip: {},
      title: { text: `${periodLabel}自习室预约占比`, left: 'center', textStyle: { fontSize: 14 } },
      series: [{ type: 'pie', radius: '70%', data: data.map(x => ({ name: x.roomName, value: x.reservationCount || x.usageRate })) }]
    }, true)
    return
  }
  const trend = adminStatsReport.value.trend || []
  if (trend.length) {
    chart.setOption({
      tooltip: {},
      title: { text: `${periodLabel}预约趋势`, left: 'center', textStyle: { fontSize: 14 } },
      xAxis: { type: 'category', data: trend.map(x => x.label) },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: trend.map(x => x.count), itemStyle: { color: '#6c5ce7' } }]
    }, true)
    return
  }
  chart.setOption({
    tooltip: {},
    title: { text: `${periodLabel}各自习室使用率`, left: 'center', textStyle: { fontSize: 14 } },
    xAxis: { type: 'category', data: data.map(x => x.roomName) },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
    series: [{ type: 'bar', data: data.map(x => x.usageRate), itemStyle: { color: '#6c5ce7' } }]
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
    return () => h('div', { class: 'card feedback-box' }, [
      h('strong', '问题反馈'),
      h('textarea', { placeholder: '描述遇到的问题或建议', value: content.value, onInput: e => { content.value = e.target.value } }),
      h('button', { class: 'primary-action small', onClick: () => { if (content.value.trim()) { emit('submit', content.value); content.value = '' } } }, '提交反馈')
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
  if (page === 'checkin') startCheckinPagePoll()
  else stopCheckinPagePoll()
}, { immediate: true })
onBeforeUnmount(() => {
  stopCameraScan()
  closeQrModal()
  stopCheckinPagePoll()
  if (studyTimerHandle) clearInterval(studyTimerHandle)
})
</script>
