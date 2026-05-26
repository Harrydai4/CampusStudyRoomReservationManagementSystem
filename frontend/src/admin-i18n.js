/** 管理端表格列名与枚举值中文映射 */

export const ADMIN_COLUMN_LABELS = {
  student_no: '学号',
  studentNo: '学号',
  studentName: '姓名',
  name: '姓名',
  college: '学院',
  credit_score: '信用分',
  auditLabel: '审核状态',
  statusLabel: '账号状态',
  reservation_no: '预约编号',
  roomName: '自习室',
  seatNo: '座位',
  reserve_date: '预约日期',
  reserveDate: '预约日期',
  status: '状态',
  checkin_time: '签到时间',
  checkout_time: '签退时间',
  result: '签到结果',
  type: '类型',
  severity: '严重程度',
  content: '内容',
  module: '模块',
  action: '操作',
  target_type: '对象类型',
  detail: '详情',
  created_at: '时间',
  startTime: '开始',
  endTime: '结束'
}

const RESERVATION_STATUS = {
  PENDING: '待签到',
  USING: '使用中',
  TEMP_LEAVE: '暂离',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  VIOLATED: '违约',
  AUTO_CHECKOUT: '自动签退',
  NO_SHOW: '未到'
}

const FEEDBACK_TYPE = { SUGGESTION: '建议', COMPLAINT: '投诉', FACILITY: '设施', OTHER: '其他' }
const FEEDBACK_SEVERITY = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '紧急' }
const FEEDBACK_STATUS = { PENDING: '待处理', PROCESSING: '处理中', DONE: '已处理', CLOSED: '已关闭' }
const CHECKIN_RESULT = { ON_TIME: '准时', LATE: '迟到', MANUAL: '人工确认', QR_SCAN: '扫码' }

export function reservationStatusText(s) {
  return RESERVATION_STATUS[s] || s || '-'
}

export function formatAdminCell(key, value, row = {}) {
  if (value == null || value === '') return '-'
  if (key === 'status') return reservationStatusText(value)
  if (key === 'type') return FEEDBACK_TYPE[value] || value
  if (key === 'severity') return FEEDBACK_SEVERITY[value] || value
  if (key === 'result') return CHECKIN_RESULT[value] || value
  if (key === 'reserve_date' || key === 'reserveDate') return String(value).slice(0, 10)
  if (key === 'checkin_time' || key === 'checkout_time' || key === 'created_at') {
    return String(value).replace('T', ' ').slice(0, 19)
  }
  if (key === 'module' || key === 'action' || key === 'target_type') {
    return LOG_LABELS[value] || value
  }
  return String(value)
}

const LOG_LABELS = {
  USER: '用户', ROOM: '自习室', AUTH: '认证', RESERVATION: '预约', FEEDBACK: '反馈',
  APPROVE: '通过', REJECT: '拒绝', CREATE: '创建', UPDATE: '更新', DELETE: '删除',
  CHANGE_PASSWORD: '改密', STUDENT: '学生', ADMIN: '管理员', STUDY_ROOM: '自习室'
}

export function decorateReservationRow(row) {
  return { ...row, status: reservationStatusText(row.status) }
}

export function decorateFeedbackRow(row) {
  return {
    ...row,
    type: FEEDBACK_TYPE[row.type] || row.type,
    severity: FEEDBACK_SEVERITY[row.severity] || row.severity,
    status: FEEDBACK_STATUS[row.status] || row.status
  }
}

export function decorateCheckinRow(row) {
  return {
    ...row,
    result: CHECKIN_RESULT[row.result] || row.result,
    checkin_time: formatAdminCell('checkin_time', row.checkin_time, row),
    checkout_time: formatAdminCell('checkout_time', row.checkout_time, row)
  }
}
