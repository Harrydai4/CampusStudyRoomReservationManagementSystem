<template>
  <div class="device" :class="{ desktop: isDesktop, mobile: !isDesktop }">
    <div v-if="toast" class="toast">{{ toast }}</div>

    <section v-if="!token" class="login-page">
      <div class="login-panel">
        <div class="login-logo">🎓</div>
        <div class="brand">校园自习室预约系统</div>
        <p>{{ loginRole === 'student' ? 'Campus Study Room Reservation' : 'Study Room Admin Console' }}</p>
        <div class="card login-card" v-if="loginRole === 'student'">
          <label>学号</label>
          <el-input v-model="studentLogin.username" placeholder="请输入学号" />
          <label>密码</label>
          <el-input v-model="studentLogin.password" type="password" show-password placeholder="请输入密码" />
          <el-button type="primary" size="large" @click="loginStudent">登录</el-button>
          <div class="login-links">
            <button @click="forgetPassword">忘记密码?</button>
            <button @click="registerOpen = true">注册账号 →</button>
          </div>
          <button class="switch-login" @click="loginRole = 'admin'">🔧 切换管理员登录</button>
        </div>
        <div class="card login-card" v-else>
          <label>管理员账号</label>
          <el-input v-model="adminLogin.account" placeholder="请输入管理员账号" />
          <label>密码</label>
          <el-input v-model="adminLogin.password" type="password" show-password placeholder="请输入密码" />
          <el-button type="primary" size="large" @click="loginAdmin">登录</el-button>
          <button class="switch-login" @click="loginRole = 'student'">🎓 切换学生登录</button>
        </div>
      </div>
    </section>

    <section v-else-if="role === 'STUDENT'" class="student-app">
      <header class="topbar">
        <button class="icon-btn" v-if="studentPage !== 'home'" @click="studentPage = 'home'">←</button>
        <h1>{{ studentTitle }}</h1>
        <button class="icon-btn bell-btn" v-if="studentPage === 'home'" @click="openNotifications">🔔<span v-if="unreadCount" class="badge">{{ unreadCount }}</span></button>
        <span v-else class="topbar-spacer"></span>
      </header>

      <main class="content">
        <template v-if="studentPage === 'home'">
          <div class="hero-card">
            <strong>你好，{{ me.name || '同学' }}</strong>
            <span>{{ homeDateText }}</span>
          </div>
          <h2>📣 公告通知</h2>
          <div class="notice-strip">
            <article class="card announcement" v-for="a in announcements.slice(0, 4)" :key="a.id" @click="readAnnouncement(a)">
              <span class="pill" :class="{ hot: a.pinned }">📌 {{ a.pinned ? '系统通知' : a.type }}</span>
              <strong>{{ a.title }}</strong>
              <p>{{ formatDate(a.published_at || a.created_at) }}</p>
            </article>
          </div>
          <h2>🗓️ 今日预约</h2>
          <article class="card today-card" v-if="todayReservation">
            <div>
              <strong>📌 今日预约</strong>
              <dl>
                <dt>自习室</dt><dd>{{ todayReservation.roomName }}</dd>
                <dt>座位</dt><dd>{{ todayReservation.seatNo }}</dd>
                <dt>时段</dt><dd>{{ timeRangeText(todayReservation) }}</dd>
              </dl>
            </div>
            <span class="status-chip">{{ statusText(todayReservation.status) }}</span>
          </article>
          <div v-else class="card empty">今天还没有预约</div>
          <h2>🏠 推荐自习室</h2>
          <div class="room-list">
            <article class="card room-card" v-for="r in rooms" :key="r.id" @click="selectRoom(r.id)">
              <span class="room-icon">🏫</span>
              <div>
                <strong>{{ r.name }}</strong>
                <p>{{ r.location }} · 普通</p>
              </div>
              <span class="seat-left">余{{ r.availableSeats }}座</span>
            </article>
          </div>
        </template>

        <template v-if="studentPage === 'reservation'">
          <h2>🗓️ 选择日期</h2>
          <div class="date-rail">
            <button v-for="d in dateOptions" :key="d.date" class="date-pill" :class="{ active: reservationForm.date === d.date }" @click="setReservationDate(d.date)">
              <span>{{ d.label }}</span>
              <strong>{{ d.day }}</strong>
              <small>{{ d.month }}月</small>
            </button>
          </div>

          <h2>⏱️ 选择时段</h2>
          <div class="card reserve-config">
            <div class="time-select-row">
              <label>开始</label>
              <el-time-select v-model="reservationForm.startTime" start="07:00" step="00:10" end="22:20" @change="handleStartTimeChange" />
              <span>→</span>
              <label>结束</label>
              <el-time-select v-model="reservationForm.endTime" :start="endSelectStart" step="00:10" end="22:30" @change="handleEndTimeChange" />
            </div>
          </div>

          <h2>🧮 预约配置</h2>
          <div class="card reserve-config">
            <label>自习室</label>
            <el-select v-model="reservationForm.roomId" @change="loadAvailableSeats">
              <el-option v-for="r in rooms" :key="r.id" :label="`${r.name}（余${r.availableSeats}）`" :value="r.id" />
            </el-select>
            <div class="legend">
              <span><i class="free"></i>可选</span><span><i class="busy"></i>不可用</span><span><i class="selected-dot"></i>已选</span>
            </div>
          </div>

          <div class="card seat-map-card">
            <div class="section-head"><strong>🗺️ 座位分布图</strong><span>{{ currentRoom?.name || '自习室' }}</span></div>
            <img v-if="roomLayoutImage" class="seat-layout-image" :src="roomLayoutImage" :alt="`${currentRoom?.name || '自习室'}座位分布图`" />
            <div v-else class="seat-layout-empty">管理员尚未上传该自习室的座位分布图</div>
          </div>

          <section class="card seat-section">
            <h3>座位选择（可预约 {{ availableSeatCount }}）</h3>
            <div class="seat-choice-grid">
              <button v-for="s in seats" :key="s.id" class="seat-choice"
                      :class="{ selected: selectedSeat?.id === s.id, disabled: !s.available }"
                      @click="openSeatDetail(s)">
                {{ s.seat_no }}
              </button>
            </div>
          </section>
          <button class="primary-action reserve-submit" :disabled="!selectedSeat" @click="createReservation">确认预约 {{ selectedSeat ? selectedSeat.seat_no : '' }}</button>
        </template>

        <template v-if="studentPage === 'checkin'">
          <div class="card check-card check-hero">
            <span class="status" :class="activeReservation?.status || 'PENDING'">{{ activeReservation ? statusText(activeReservation.status) : '暂无预约' }}</span>
            <div class="timer">{{ timerText }}</div>
          </div>
          <div class="card reservation-detail-card" v-if="activeReservation">
            <dl>
              <dt>自习室</dt><dd>{{ activeReservation.roomName }}</dd>
              <dt>座位号</dt><dd>{{ activeReservation.seatNo }}</dd>
              <dt>预约时段</dt><dd>{{ timeRangeText(activeReservation) }}</dd>
              <dt>预约日期</dt><dd>{{ formatDate(activeReservation.reserve_date) }}</dd>
            </dl>
          </div>
          <div class="card check-actions">
            <button class="round-action primary-round" :disabled="!activeReservation || activeReservation.status !== 'PENDING'" @click="loadQr">
              <span>🔢</span><strong>出示二维码签到</strong>
            </button>
            <button class="round-action warning-round" @click="studentPage = 'feedback'">
              <span>💬</span><strong>问题反馈</strong>
            </button>
            <el-button class="checkout-btn" :disabled="!activeReservation || activeReservation.status !== 'USING'" @click="checkout">签退</el-button>
            <div class="check-hint">请在预约开始前后 15 分钟内完成签到。</div>
          </div>
          <div class="card qr-card" v-if="qrToken">
            <div class="qr-image" v-html="qrSvg"></div>
            <div class="qr-meta">
              <strong>{{ qrInfo.name }} · {{ qrInfo.studentNo }}</strong>
              <span>{{ qrInfo.roomName }} · {{ qrInfo.seatNo }}</span>
              <small>二维码 {{ qrInfo.expireSeconds || 60 }} 秒内有效，请向管理员出示</small>
            </div>
          </div>
        </template>

        <template v-if="studentPage === 'profile'">
          <div class="card profile-card">
            <div class="avatar">{{ (me.name || '同').slice(0, 1) }}</div>
            <div>
              <strong>{{ me.name || '同学' }}</strong>
              <span>学号：{{ me.student_no || me.username }}</span>
              <span>{{ me.college || '计算机学院' }}</span>
            </div>
          </div>
          <h2>功能服务</h2>
          <div class="menu-list">
            <button @click="studentPage = 'myres'"><span>📋</span>我的预约<i>›</i></button>
            <button @click="studentPage = 'credit'"><span>⭐</span>信用积分<i>›</i></button>
            <button @click="studentPage = 'stats'; drawStudentChart()"><span>📊</span>学习统计<i>›</i></button>
          </div>
          <h2>账号与安全</h2>
          <div class="menu-list">
            <button @click="studentPage = 'settings'"><span>📝</span>个人信息<i>›</i></button>
            <button @click="notify('修改密码功能待接入')"><span>🔐</span>修改密码<i>›</i></button>
            <button @click="studentPage = 'settings'"><span>⚙️</span>设置<i>›</i></button>
            <button @click="notify('校园自习室预约系统')"><span>ℹ️</span>关于系统<i>›</i></button>
            <button @click="studentPage = 'feedback'"><span>💬</span>问题反馈<i>›</i></button>
            <button class="logout-row" @click="logout"><span>🚪</span>退出登录<i>›</i></button>
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
          <h2>📜 积分变动记录</h2>
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
          <article class="card note" v-for="n in notifications" :key="n.id" @click="readNotification(n)">
            <span class="dot" :class="{ read: n.read_flag }"></span>
            <strong>{{ n.title }}</strong>
            <p>{{ n.content }}</p>
          </article>
        </template>

        <template v-if="studentPage === 'settings'">
          <div class="card">当前账号：{{ me.student_no }}，资料状态：{{ me.audit_status }}</div>
        </template>
      </main>

      <nav class="bottom-nav">
        <button v-for="n in studentNav" :key="n.page" :class="{ active: activeStudentTab === n.page }" @click="studentPage = n.page">
          <span>{{ n.icon }}</span>
          <b>{{ n.label }}</b>
        </button>
      </nav>
    </section>

    <section v-else class="admin-app">
      <aside class="sidebar" v-if="isDesktop">
        <strong>管理后台</strong>
        <button v-for="n in adminNav" :key="n.page" :class="{ active: adminPage === n.page }" @click="openAdmin(n.page)">{{ n.label }}</button>
        <button @click="logout">退出</button>
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
            <div class="dashboard-grid">
              <div class="metric"><b>{{ dashboard.roomCount }}</b><span>自习室</span></div>
              <div class="metric"><b>{{ dashboard.pendingUsers }}</b><span>待审核</span></div>
              <div class="metric"><b>{{ dashboard.todayReservations }}</b><span>今日预约</span></div>
              <div class="metric"><b>{{ dashboard.usingCount }}</b><span>使用中</span></div>
            </div>
            <div class="card"><div ref="adminChart" class="chart"></div></div>
          </template>

          <template v-if="adminPage === 'users'">
            <el-input v-model="userKeyword" placeholder="搜索学号或姓名" @input="loadUsers" />
            <DataTable :rows="users" :columns="['student_no','name','college','audit_status','accountStatus']">
              <template #actions="{ row }">
                <el-button size="small" @click="approve(row.userId)">通过</el-button>
                <el-button size="small" @click="reject(row.userId)">拒绝</el-button>
                <el-button size="small" @click="disable(row.userId)">禁用</el-button>
                <el-button size="small" @click="enable(row.userId)">启用</el-button>
              </template>
            </DataTable>
          </template>

          <template v-if="adminPage === 'rooms'">
            <el-button type="primary" @click="editRoom()">新增自习室</el-button>
            <article class="card room-card" v-for="r in rooms" :key="r.id">
              <div><strong>{{ r.name }}</strong><p>{{ r.location }} · {{ r.status }} · {{ r.availableSeats }} 可用</p></div>
              <span>
                <el-button size="small" @click="editRoom(r)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteRoom(r)">删除</el-button>
              </span>
            </article>
          </template>

          <template v-if="adminPage === 'seats'">
            <el-select v-model="adminSeatRoomId" @change="loadAdminSeats">
              <el-option v-for="r in rooms" :key="r.id" :label="r.name" :value="r.id" />
            </el-select>
            <div class="seat-grid admin-seat-grid" :style="{ gridTemplateColumns: `repeat(${adminRoom?.col_count || 6}, minmax(0, 1fr))` }">
              <button v-for="s in adminSeats" :key="s.id" class="seat" :class="s.status === 'NORMAL' ? 'free' : 'disabled'" @click="toggleSeat(s)">
                <strong>{{ s.seat_no }}</strong><small>{{ s.status }}</small>
              </button>
            </div>
          </template>

          <template v-if="adminPage === 'reservations'">
            <DataTable :rows="adminReservations" :columns="['reservation_no','studentName','roomName','seatNo','reserve_date','status']" />
          </template>

          <template v-if="adminPage === 'checkins'">
            <div class="card scan-box">
              <div class="scanner-toolbar">
                <el-button type="primary" @click="isScanning ? stopCameraScan() : startCameraScan()">
                  {{ isScanning ? '停止扫码' : '扫码签到' }}
                </el-button>
                <el-button :disabled="!scanToken.trim()" @click="scanCheckin">手动确认</el-button>
              </div>
              <div class="scanner-view" v-if="isScanning">
                <video ref="scanVideo" autoplay muted playsinline></video>
                <div class="scanner-frame"></div>
              </div>
              <p class="scanner-hint">{{ scanHint }}</p>
              <el-input v-model="scanToken" placeholder="摄像头无法自动识别时，可粘贴学生二维码 token" />
            </div>
            <DataTable :rows="checkins" :columns="['studentName','roomName','seatNo','checkin_time','checkout_time','result']" />
          </template>

          <template v-if="adminPage === 'announcements'">
            <el-button type="primary" @click="editAnnouncement()">发布公告</el-button>
            <article class="card announcement" v-for="a in announcements" :key="a.id">
              <strong>{{ a.title }}</strong><p>{{ a.content }}</p>
              <el-button size="small" @click="editAnnouncement(a)">编辑</el-button>
            </article>
          </template>

          <template v-if="adminPage === 'statistics'">
            <el-button @click="downloadReport">导出报表</el-button>
            <div class="card"><div ref="usageChart" class="chart"></div></div>
          </template>

          <template v-if="adminPage === 'feedback'">
            <DataTable :rows="adminFeedback" :columns="['studentName','roomName','seatNo','type','severity','content','status']">
              <template #actions="{ row }">
                <el-button size="small" @click="handleFeedback(row.id)">标记处理</el-button>
              </template>
            </DataTable>
          </template>

          <template v-if="adminPage === 'settings'">
            <div class="card">当前管理员：{{ me.name }} · {{ me.role }}</div>
          </template>
        </main>
      </div>
    </section>

    <el-dialog v-model="registerOpen" title="注册账号" width="min(92vw, 560px)">
      <div class="dialog-form">
        <el-input v-model="registerForm.studentNo" placeholder="学号" />
        <el-input v-model="registerForm.name" placeholder="姓名" />
        <el-input v-model="registerForm.college" placeholder="学院" />
        <el-input v-model="registerForm.major" placeholder="专业" />
        <el-input v-model="registerForm.grade" placeholder="年级" />
        <el-input v-model="registerForm.phone" placeholder="手机号" />
        <el-input v-model="registerForm.email" placeholder="邮箱" />
        <el-input v-model="registerForm.password" type="password" show-password placeholder="密码" />
      </div>
      <template #footer><el-button type="primary" @click="register">提交注册</el-button></template>
    </el-dialog>

    <el-dialog v-model="roomDialog" title="自习室信息" width="min(92vw, 620px)">
      <div class="dialog-form">
        <el-input v-model="roomForm.roomCode" placeholder="编号" />
        <el-input v-model="roomForm.name" placeholder="名称" />
        <el-input v-model="roomForm.location" placeholder="位置" />
        <el-input v-model="roomForm.floor" placeholder="楼层" />
        <el-input v-model="roomForm.openTime" placeholder="开放开始 07:00:00" />
        <el-input v-model="roomForm.closeTime" placeholder="开放结束 22:30:00" />
        <el-input v-model="roomForm.layoutImageUrl" placeholder="座位分布图地址（管理端上传后填写）" />
        <el-input-number v-model="roomForm.rowCount" :min="1" :max="20" />
        <el-input-number v-model="roomForm.colCount" :min="1" :max="20" />
      </div>
      <template #footer><el-button type="primary" @click="saveRoom">保存</el-button></template>
    </el-dialog>

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
</template>

<script setup>
import axios from 'axios'
import * as echarts from 'echarts'
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { createQrSvg } from './qr'

const api = axios.create({ baseURL: '/api' })
const token = ref(localStorage.getItem('token') || '')
const role = ref(localStorage.getItem('role') || '')
api.interceptors.request.use(config => {
  if (token.value) config.headers.Authorization = `Bearer ${token.value}`
  return config
})
api.interceptors.response.use(res => {
  if (res.data && typeof res.data.code === 'number' && res.data.code !== 200) {
    throw new Error(res.data.message)
  }
  return res
})

const width = ref(window.innerWidth)
const isDesktop = computed(() => width.value >= 900)
const toast = ref('')
const me = ref({})
const loginRole = ref('student')
const studentLogin = reactive({ username: '202301010101', password: '123456' })
const adminLogin = reactive({ account: 'admin', password: 'admin123' })
const registerOpen = ref(false)
const registerForm = reactive({ studentNo: '', name: '', gender: '男', college: '计算机科学与技术学院', major: '软件工程', grade: '2023级', phone: '', email: '', password: '' })

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
const qrInfo = ref({})
const credit = ref({ score: 0, logs: [] })
const studyStats = ref({})
const studentChart = ref(null)
const statPeriod = ref('day')
const announcementDetailOpen = ref(false)
const activeAnnouncement = ref({})
const seatDialogOpen = ref(false)
const pendingSeat = ref(null)

const adminPage = ref('dashboard')
const dashboard = ref({})
const users = ref([])
const userKeyword = ref('')
const adminReservations = ref([])
const checkins = ref([])
const adminFeedback = ref([])
const adminSeatRoomId = ref(null)
const adminSeats = ref([])
const scanToken = ref('')
const scanVideo = ref(null)
const isScanning = ref(false)
const scanHint = ref('手机端可打开摄像头扫描学生二维码，也可手动粘贴 token。')
let scanStream = null
let scanFrame = 0
let barcodeDetector = null
const adminChart = ref(null)
const usageChart = ref(null)
const roomDialog = ref(false)
const roomForm = reactive({})
const announcementDialog = ref(false)
const announcementForm = reactive({})

const todayText = computed(() => new Date().toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' }))
const homeDateText = computed(() => `${new Date().getMonth() + 1}月${new Date().getDate()}日`)
const unreadCount = computed(() => notifications.value.filter(n => !n.read_flag).length)
const currentRoom = computed(() => rooms.value.find(r => r.id === reservationForm.roomId))
const adminRoom = computed(() => rooms.value.find(r => r.id === adminSeatRoomId.value))
const todayReservation = computed(() => reservations.value.find(r => String(r.reserve_date).startsWith(reservationForm.date) && ['PENDING', 'USING'].includes(r.status)))
const activeReservation = computed(() => reservations.value.find(r => ['PENDING', 'USING'].includes(r.status)))
const timerText = computed(() => activeReservation.value?.status === 'USING' ? '使用中' : '00:00:00')
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
const creditPercent = computed(() => Math.max(0, Math.min(100, Math.round((Number(credit.value.score || 0) / 300) * 100))))
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
  month: '本月每日学习时长（小时）'
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

const statPeriods = [{ key: 'day', label: '日报' }, { key: 'week', label: '周报' }, { key: 'month', label: '月报' }]
const reservationTabs = [{ key: 'ALL', label: '全部' }, { key: 'PENDING', label: '待使用' }, { key: 'USING', label: '使用中' }, { key: 'COMPLETED', label: '已完成' }, { key: 'CANCELLED', label: '已取消' }]
const studentNav = [{ page: 'home', label: '首页', icon: '🏠' }, { page: 'reservation', label: '预约', icon: '🪑' }, { page: 'checkin', label: '签到', icon: '✅' }, { page: 'profile', label: '我的', icon: '👤' }]
const adminNav = [{ page: 'dashboard', label: '仪表盘' }, { page: 'users', label: '用户管理' }, { page: 'rooms', label: '自习室' }, { page: 'seats', label: '座位' }, { page: 'reservations', label: '预约' }, { page: 'checkins', label: '签到' }, { page: 'announcements', label: '公告' }, { page: 'statistics', label: '统计' }, { page: 'feedback', label: '反馈' }, { page: 'settings', label: '设置' }]

function notify(message) {
  toast.value = message
  setTimeout(() => { toast.value = '' }, 2200)
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
function forgetPassword() {
  notify('请联系管理员重置密码')
}
async function call(method, url, data, config) {
  const res = await api.request({ method, url, data, ...config })
  return res.data.data
}
async function loginStudent() {
  try {
    const data = await call('post', '/auth/login', studentLogin)
    afterLogin(data.token, 'STUDENT', data.userInfo)
  } catch (e) { notify(e.message) }
}
async function loginAdmin() {
  try {
    const data = await call('post', '/admin/auth/login', adminLogin)
    afterLogin(data.token, data.adminInfo.role === 'SUPER_ADMIN' ? 'SUPER_ADMIN' : 'ADMIN', data.adminInfo)
  } catch (e) { notify(e.message) }
}
function afterLogin(t, r, info) {
  token.value = t
  role.value = r
  me.value = info || {}
  localStorage.setItem('token', t)
  localStorage.setItem('role', r)
  notify('登录成功')
  bootstrap()
}
async function register() {
  try {
    await call('post', '/auth/register', registerForm)
    registerOpen.value = false
    notify('注册申请已提交，请等待管理员审核')
  } catch (e) { notify(e.message) }
}
function logout() {
  stopCameraScan()
  token.value = ''
  role.value = ''
  localStorage.clear()
}
async function bootstrap() {
  if (!token.value) return
  try {
    me.value = await call('get', '/auth/me')
    await Promise.all([loadRooms(), loadAnnouncements()])
    if (role.value === 'STUDENT') {
      await Promise.all([loadReservations(), loadNotifications(), loadCredit(), loadStudyStats()])
    } else {
      await openAdmin('dashboard')
    }
  } catch (e) {
    notify(e.message)
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
  pendingSeat.value = seat
  seatDialogOpen.value = true
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
  try {
    const data = await call('get', '/checkin/qrcode')
    qrToken.value = data.qrToken
    qrInfo.value = data
    qrSvg.value = createQrSvg(data.qrToken)
    notify('签到二维码已生成')
  } catch (e) { notify(e.message) }
}
async function checkout() {
  try {
    await call('post', `/reservations/${activeReservation.value.id}/checkout`)
    notify('签退成功')
    await loadReservations()
  } catch (e) { notify(e.message) }
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
  return { PENDING: '待使用', USING: '使用中', COMPLETED: '已完成', CANCELLED: '已取消', VIOLATED: '违约', AUTO_CANCELLED: '超时取消', AUTO_CHECKOUT: '自动签退' }[status] || status
}
async function openAdmin(page) {
  if (page !== 'checkins') stopCameraScan()
  adminPage.value = page
  if (page === 'dashboard') {
    dashboard.value = await call('get', '/admin/dashboard')
    await nextTick()
    drawAdminChart()
  }
  if (page === 'users') await loadUsers()
  if (page === 'rooms') await loadRooms()
  if (page === 'seats') await loadAdminSeats()
  if (page === 'reservations') adminReservations.value = await call('get', '/admin/reservations')
  if (page === 'checkins') checkins.value = await call('get', '/admin/checkins')
  if (page === 'announcements') await loadAnnouncements()
  if (page === 'statistics') {
    dashboard.value.usage = await call('get', '/admin/statistics/usage')
    await nextTick()
    drawUsageChart()
  }
  if (page === 'feedback') adminFeedback.value = await call('get', '/admin/feedback')
}
async function loadUsers() {
  users.value = await call('get', '/admin/users', null, { params: { keyword: userKeyword.value } })
}
async function approve(id) { await call('post', `/admin/users/${id}/approve`, {}); notify('审核通过'); loadUsers() }
async function reject(id) { await call('post', `/admin/users/${id}/reject`, { remark: '资料不符合要求' }); notify('已拒绝'); loadUsers() }
async function disable(id) { await call('post', `/admin/users/${id}/disable`); notify('已禁用'); loadUsers() }
async function enable(id) { await call('post', `/admin/users/${id}/enable`); notify('已启用'); loadUsers() }
function editRoom(r = {}) {
  Object.assign(roomForm, { id: r.id, roomCode: r.room_code || '', name: r.name || '', location: r.location || '', floor: r.floor || '1楼', openTime: String(r.open_time || '07:00:00'), closeTime: String(r.close_time || '22:30:00'), layoutImageUrl: r.layout_image_url || '', rowCount: r.row_count || 4, colCount: r.col_count || 6, status: r.status || 'OPEN' })
  roomDialog.value = true
}
async function saveRoom() {
  const method = roomForm.id ? 'put' : 'post'
  const url = roomForm.id ? `/admin/rooms/${roomForm.id}` : '/admin/rooms'
  await call(method, url, roomForm)
  roomDialog.value = false
  notify('自习室已保存')
  await loadRooms()
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
  try {
    await call('post', '/admin/checkin/scan', { qrToken: scanToken.value.trim() })
    notify('签到成功')
    scanToken.value = ''
    checkins.value = await call('get', '/admin/checkins')
  } catch (e) { notify(e.message) }
}
async function startCameraScan() {
  try {
    if (!navigator.mediaDevices?.getUserMedia) {
      scanHint.value = '当前浏览器无法调用摄像头，请改用手动粘贴 token。'
      notify(scanHint.value)
      return
    }
    scanStream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: { facingMode: { ideal: 'environment' } }
    })
    isScanning.value = true
    scanHint.value = '摄像头已打开，请将学生签到二维码放入扫描框内。'
    await nextTick()
    scanVideo.value.srcObject = scanStream
    await scanVideo.value.play()

    if (!('BarcodeDetector' in window)) {
      scanHint.value = '摄像头已打开，但当前浏览器不支持自动识别二维码；请使用手机 Chrome 或手动粘贴 token。'
      notify('摄像头已打开，当前浏览器不支持自动识别')
      return
    }
    const supportedFormats = await window.BarcodeDetector.getSupportedFormats?.()
    if (supportedFormats && !supportedFormats.includes('qr_code')) {
      scanHint.value = '摄像头已打开，但当前浏览器不支持 QR 识别；请改用手动粘贴 token。'
      notify('当前浏览器不支持 QR 自动识别')
      return
    }

    barcodeDetector = new window.BarcodeDetector({ formats: ['qr_code'] })
    scanCameraFrame()
  } catch (e) {
    stopCameraScan()
    scanHint.value = '摄像头启动失败，请检查浏览器权限或改用手动粘贴 token。'
    notify(scanHint.value)
  }
}
async function scanCameraFrame() {
  if (!isScanning.value || !barcodeDetector || !scanVideo.value) return
  try {
    const codes = await barcodeDetector.detect(scanVideo.value)
    if (codes.length > 0) {
      scanToken.value = codes[0].rawValue
      stopCameraScan()
      await scanCheckin()
      return
    }
  } catch (e) {
    scanHint.value = '正在读取画面，请保持二维码清晰。'
  }
  scanFrame = requestAnimationFrame(scanCameraFrame)
}
function stopCameraScan() {
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
  scanHint.value = '手机端可打开摄像头扫描学生二维码，也可手动粘贴 token。'
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
async function handleFeedback(id) {
  await call('put', `/admin/feedback/${id}`, { status: 'DONE', handleResult: '已处理并记录' })
  notify('反馈已处理')
  adminFeedback.value = await call('get', '/admin/feedback')
}
function downloadReport() {
  api.get('/admin/statistics/export', { responseType: 'blob' }).then(res => {
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
  const data = dashboard.value.usage || []
  if (adminChart.value) echarts.init(adminChart.value).setOption({ tooltip: {}, xAxis: { type: 'category', data: data.map(x => x.roomName) }, yAxis: { type: 'value' }, series: [{ type: 'bar', data: data.map(x => x.usageRate), itemStyle: { color: '#6c5ce7' } }] })
}
function drawUsageChart() {
  const data = dashboard.value.usage || []
  if (usageChart.value) echarts.init(usageChart.value).setOption({ tooltip: {}, series: [{ type: 'pie', radius: '70%', data: data.map(x => ({ name: x.roomName, value: x.reservationCount })) }] })
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
  props: { rows: Array, columns: Array },
  setup(props, { slots }) {
    return () => h('div', { class: 'table-wrap' }, h('table', [
      h('thead', h('tr', [...props.columns.map(c => h('th', c)), h('th', '操作')])),
      h('tbody', (props.rows || []).map(row => h('tr', [...props.columns.map(c => h('td', String(row[c] ?? ''))), h('td', slots.actions ? slots.actions({ row }) : '')])))
    ]))
  }
})

window.addEventListener('resize', () => { width.value = window.innerWidth })
onMounted(bootstrap)
onBeforeUnmount(stopCameraScan)
</script>
