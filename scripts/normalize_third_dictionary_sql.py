#!/usr/bin/env python3
"""Regenerate SQL dumps so they match the third-version data dictionary.

The source dump is the existing full-year demo dataset. This script keeps that
dataset size, normalizes dictionary values, splits room facilities, removes
physical derived columns, and writes schema.sql, data.sql, and database-full.sql.
"""

from __future__ import annotations

from collections import OrderedDict, defaultdict
from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]
CFG_DIR = ROOT / "docs" / "06-部署配置"
FULL_SQL = CFG_DIR / "database-full.sql"
SCHEMA_SQL = CFG_DIR / "schema.sql"
DATA_SQL = CFG_DIR / "data.sql"

FACILITIES = ["空调", "WiFi", "电源插座", "饮水机", "白板", "投影设备"]


def find_statement_end(text: str, pos: int) -> int:
    quoted = False
    escaped = False
    i = pos
    while i < len(text):
        ch = text[i]
        if quoted:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == "'":
                quoted = False
        else:
            if ch == "'":
                quoted = True
            elif ch == ";":
                return i
        i += 1
    raise ValueError("unterminated INSERT statement")


def parse_sql_string(text: str, pos: int) -> tuple[str, int]:
    assert text[pos] == "'"
    pos += 1
    out: list[str] = []
    escapes = {"0": "\0", "n": "\n", "r": "\r", "t": "\t", "b": "\b", "Z": "\x1a"}
    while pos < len(text):
        ch = text[pos]
        if ch == "\\":
            pos += 1
            if pos >= len(text):
                break
            out.append(escapes.get(text[pos], text[pos]))
        elif ch == "'":
            return "".join(out), pos + 1
        else:
            out.append(ch)
        pos += 1
    raise ValueError("unterminated SQL string")


def parse_bare_value(token: str):
    token = token.strip()
    if token.upper() == "NULL":
        return None
    if re.fullmatch(r"-?\d+", token):
        return int(token)
    return token


def parse_values(body: str) -> list[list[object]]:
    rows: list[list[object]] = []
    i = 0
    n = len(body)
    while i < n:
        while i < n and body[i] in " \r\n\t,":
            i += 1
        if i >= n:
            break
        if body[i] != "(":
            raise ValueError(f"expected row at offset {i}")
        i += 1
        row: list[object] = []
        while i < n:
            while i < n and body[i] in " \r\n\t":
                i += 1
            if body[i] == "'":
                value, i = parse_sql_string(body, i)
            else:
                start = i
                while i < n and body[i] not in ",)":
                    i += 1
                value = parse_bare_value(body[start:i])
            row.append(value)
            while i < n and body[i] in " \r\n\t":
                i += 1
            if i < n and body[i] == ",":
                i += 1
                continue
            if i < n and body[i] == ")":
                i += 1
                rows.append(row)
                break
        while i < n and body[i] in " \r\n\t,":
            i += 1
    return rows


def parse_inserts(text: str) -> OrderedDict[str, list[dict[str, object]]]:
    tables: OrderedDict[str, list[dict[str, object]]] = OrderedDict()
    pattern = re.compile(r"INSERT INTO `([^`]+)` \((.*?)\) VALUES", re.S)
    for match in pattern.finditer(text):
        table = match.group(1)
        columns = re.findall(r"`([^`]+)`", match.group(2))
        end = find_statement_end(text, match.end())
        body = text[match.end() : end]
        for values in parse_values(body):
            if len(values) != len(columns):
                raise ValueError(f"{table}: {len(values)} values for {len(columns)} columns")
            tables.setdefault(table, []).append(dict(zip(columns, values)))
    return tables


def sql_value(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, int):
        return str(value)
    text = str(value)
    text = (
        text.replace("\\", "\\\\")
        .replace("\0", "\\0")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    )
    return f"'{text}'"


def normalize_user_role(value: object) -> str:
    return {"STUDENT": "学生", "学生": "学生"}.get(str(value), str(value))


def normalize_account_status(value: object) -> str:
    return {
        "NORMAL": "正常",
        "PENDING": "待审核",
        "DISABLED": "禁用",
        "BLACKLIST": "黑名单",
        "已禁用": "禁用",
    }.get(str(value), str(value))


def normalize_admin_role(value: object) -> str:
    return {
        "ADMIN": "普通管理员",
        "NORMAL_ADMIN": "普通管理员",
        "SUPER_ADMIN": "超级管理员",
    }.get(str(value), str(value))


def normalize_admin_status(value: object) -> str:
    return {"NORMAL": "正常", "DISABLED": "离职", "禁用": "离职", "已禁用": "离职"}.get(
        str(value), str(value)
    )


def normalize_audit_status(value: object) -> str:
    return {"PENDING": "待审核", "APPROVED": "已通过", "REJECTED": "已拒绝"}.get(
        str(value), str(value)
    )


def normalize_room_status(value: object) -> str:
    return {"OPEN": "开放", "CLOSED": "关闭", "MAINTENANCE": "维护中", "MAINTAINING": "维护中"}.get(
        str(value), str(value)
    )


def normalize_seat_status(value: object) -> str:
    return {"NORMAL": "空闲", "DAMAGED": "维修", "MAINTAINING": "维修", "DISABLED": "停用"}.get(
        str(value), str(value)
    )


def normalize_reservation_status(value: object) -> str:
    return {
        "PENDING": "待使用",
        "USING": "使用中",
        "COMPLETED": "已完成",
        "AUTO_CHECKOUT": "已完成",
        "CANCELLED": "已取消",
        "VIOLATED": "已违约",
        "AUTO_CANCELLED": "已违约",
    }.get(str(value), str(value))


def normalize_slot_status(value: object) -> str:
    return {"ACTIVE": "占用", "RELEASED": "释放"}.get(str(value), str(value))


def normalize_checkin_method(value: object) -> str:
    return {"QR_SCAN": "扫码签到", "STUDENT_NO": "学号签到", "MANUAL": "人工确认"}.get(
        str(value), str(value)
    )


def normalize_checkin_result(value: object) -> str:
    return {"ON_TIME": "准时", "LATE": "迟到", "INVALID": "无效"}.get(str(value), str(value))


def normalize_credit_type(value: object) -> str:
    return {
        "ON_TIME_CHECKIN": "签到奖励",
        "NO_SHOW": "违约扣减",
        "USER_CANCEL": "违约扣减",
        "BLACKLIST_RELEASE": "系统恢复",
        "VIOLATION_REVOKE": "系统恢复",
        "INVALID_CHECKIN_REVERT": "其他",
        "NO_CHECKIN": "其他",
    }.get(str(value), str(value))


def normalize_blacklist_status(value: object) -> str:
    return {"ACTIVE": "生效", "RELEASED": "已解除"}.get(str(value), str(value))


def normalize_announcement_type(value: object) -> str:
    return {
        "SYSTEM": "系统通知",
        "RULE": "使用规则",
        "MAINTENANCE": "维护公告",
        "ACTIVITY": "其他",
        "OTHER": "其他",
    }.get(str(value), str(value))


def normalize_announcement_status(value: object) -> str:
    return {"PUBLISHED": "已发布", "DELETED": "已删除"}.get(str(value), str(value))


def normalize_notice_scope(value: object) -> str:
    return {"GLOBAL": "全局", "ROOM": "自习室"}.get(str(value), str(value))


def normalize_notification_type(value: object) -> str:
    return {
        "RESERVATION": "预约",
        "ANNOUNCEMENT": "公告",
        "CREDIT": "信用",
        "SYSTEM": "系统",
        "BLACKLIST": "黑名单",
        "CHECKOUT": "预约",
        "VIOLATION": "信用",
    }.get(str(value), str(value))


def normalize_feedback_type(value: object) -> str:
    return {
        "SUGGESTION": "建议",
        "COMPLAINT": "投诉",
        "FACILITY": "设施",
        "SEAT_REPAIR": "座位报修",
        "ENVIRONMENT": "环境",
        "NOISE": "环境",
        "SYSTEM": "系统",
        "OTHER": "其他",
    }.get(str(value), str(value))


def normalize_feedback_severity(value: object) -> str:
    return {"LOW": "低", "MEDIUM": "中", "HIGH": "高", "CRITICAL": "紧急"}.get(
        str(value), str(value)
    )


def normalize_feedback_status(value: object) -> str:
    return {"PENDING": "待处理", "PROCESSING": "待处理", "DONE": "已处理", "CLOSED": "已处理"}.get(
        str(value), str(value)
    )


def normalize_cell_category(value: object) -> str:
    return {"SEAT": "座位", "NON_SEAT": "非座位"}.get(str(value), str(value))


def normalize_operation_module(value: object) -> str:
    return {
        "AUTH": "认证",
        "USER": "用户",
        "ROOM": "自习室",
        "SEAT": "座位",
        "RESERVATION": "预约",
        "FEEDBACK": "反馈",
        "ADMIN": "管理员",
    }.get(str(value), str(value))


def normalize_operation_action(value: object) -> str:
    return {
        "CHANGE_PASSWORD": "修改密码",
        "APPROVE": "通过",
        "REJECT": "拒绝",
        "CREATE": "新增",
        "UPDATE": "更新",
        "DELETE": "删除",
        "REVOKE_VIOLATION": "撤销违约",
        "HANDLE": "处理",
        "ENABLE": "启用",
        "DISABLE": "禁用",
    }.get(str(value), str(value))


def normalize_operation_target(value: object) -> str:
    if value is None:
        return value
    return {
        "STUDENT": "学生",
        "ADMIN": "管理员",
        "STUDY_ROOM": "自习室",
        "SEAT": "座位",
        "RESERVATION": "预约",
        "FEEDBACK": "反馈",
        "ADMIN_ACCOUNT": "管理员账号",
        "USER": "用户",
    }.get(str(value), str(value))


def normalize_seat_type(value: object, quiet_zone: object = 0) -> str:
    text = str(value or "")
    if "静" in text or "安静" in text or int(quiet_zone or 0) == 1:
        return "静音"
    if "舒适" in text or "沙发" in text or "靠窗" in text:
        return "舒适"
    return "普通"


def normalize_room_code(value: object, used: set[str]) -> str:
    base = re.sub(r"[^A-Za-z0-9]", "", str(value or "").upper())[:10] or "ROOM"
    code = base
    index = 1
    while code in used:
        suffix = str(index)
        code = f"{base[: 10 - len(suffix)]}{suffix}"
        index += 1
    used.add(code)
    return code


def infer_room_type(row: dict[str, object]) -> str:
    text = "".join(str(row.get(k, "")) for k in ("name", "location", "facilities"))
    if "考研" in text:
        return "考研专区"
    if any(word in text for word in ("讨论", "研讨", "共享")):
        return "讨论"
    if any(word in text for word in ("静音", "安静")):
        return "静音"
    return "普通"


def normalize_facility(value: object) -> str:
    text = str(value or "").strip()
    if not text:
        return ""
    if text.lower() == "wifi":
        return "WiFi"
    if "空调" in text:
        return "空调"
    if any(word in text for word in ("电源", "插座", "充电", "台灯")):
        return "电源插座"
    if "饮水" in text:
        return "饮水机"
    if any(word in text for word in ("白板", "黑板")):
        return "白板"
    if any(word in text for word in ("投影", "双屏")):
        return "投影设备"
    if "讨论" in text or "研讨" in text:
        return "白板"
    return ""


def room_prefix(room_id: object) -> str:
    idx = int(room_id or 1) - 1
    if 0 <= idx < 26:
        return chr(ord("A") + idx)
    return f"R{int(room_id)}"


def trim_columns(row: dict[str, object], columns: list[str]) -> dict[str, object]:
    return {col: row.get(col) for col in columns}


def normalize_tables(tables: OrderedDict[str, list[dict[str, object]]]) -> OrderedDict[str, list[dict[str, object]]]:
    out: OrderedDict[str, list[dict[str, object]]] = OrderedDict()

    admin_rows = []
    for row in tables.get("admin_account", []):
        row = row.copy()
        row["role"] = normalize_admin_role(row.get("role"))
        row["status"] = normalize_admin_status(row.get("status"))
        admin_rows.append(row)
    out["admin_account"] = admin_rows

    user_rows = []
    for row in tables.get("user_account", []):
        row = row.copy()
        row["role"] = normalize_user_role(row.get("role"))
        row["status"] = normalize_account_status(row.get("status"))
        user_rows.append(row)
    out["user_account"] = user_rows

    profiles = []
    for row in tables.get("student_profile", []):
        row = row.copy()
        row["grade"] = re.sub(r"\D", "", str(row.get("grade") or ""))[:4]
        row["audit_status"] = normalize_audit_status(row.get("audit_status"))
        row["credit_score"] = max(0, min(500, int(row.get("credit_score") or 0)))
        profiles.append(row)
    out["student_profile"] = profiles

    used_room_codes: set[str] = set()
    rooms = []
    room_facility_names: dict[int, list[str]] = {}
    for row in tables.get("study_room", []):
        original = row.copy()
        row = row.copy()
        row["room_code"] = normalize_room_code(row.get("room_code"), used_room_codes)
        row["room_type"] = infer_room_type(original)
        row["status"] = normalize_room_status(row.get("status"))
        raw_facilities = re.split(r"[,，、/\s]+", str(original.get("facilities") or ""))
        names = []
        for item in raw_facilities:
            normalized = normalize_facility(item)
            if normalized and normalized not in names:
                names.append(normalized)
        if not names:
            names = ["空调", "WiFi"]
        room_facility_names[int(row["id"])] = names
        rooms.append(
            trim_columns(
                row,
                [
                    "id",
                    "room_code",
                    "name",
                    "room_type",
                    "location",
                    "floor",
                    "open_time",
                    "close_time",
                    "status",
                    "manager_id",
                    "row_count",
                    "col_count",
                    "layout_image_url",
                    "created_at",
                    "updated_at",
                ],
            )
        )
    out["study_room"] = rooms

    facility_rows = [
        {"id": idx, "name": name, "created_at": "2025-05-20 09:30:00"}
        for idx, name in enumerate(FACILITIES, start=1)
    ]
    facility_ids = {row["name"]: row["id"] for row in facility_rows}
    room_facility_rows = []
    for room_id, names in room_facility_names.items():
        for name in names:
            if name in facility_ids:
                room_facility_rows.append({"room_id": room_id, "facility_id": facility_ids[name]})
    out["facility"] = facility_rows
    out["study_room_facility"] = room_facility_rows

    seats = []
    seat_seq: defaultdict[int, int] = defaultdict(int)
    for row in tables.get("seat", []):
        row = row.copy()
        room_id = int(row.get("room_id") or 0)
        seat_seq[room_id] += 1
        row["seat_no"] = f"{room_prefix(room_id)}-{seat_seq[room_id]:03d}"
        row["cell_category"] = normalize_cell_category(row.get("cell_category"))
        row["seat_type"] = normalize_seat_type(row.get("seat_type"), row.get("quiet_zone"))
        row["status"] = normalize_seat_status(row.get("status"))
        seats.append(row)
    out["seat"] = seats
    seat_by_id = {row["id"]: row for row in seats}

    announcements = []
    for row in tables.get("announcement", []):
        row = row.copy()
        row["type"] = normalize_announcement_type(row.get("type"))
        row["scope"] = normalize_notice_scope(row.get("scope"))
        row["status"] = normalize_announcement_status(row.get("status"))
        announcements.append(row)
    out["announcement"] = announcements

    reservations = []
    reservation_seq: defaultdict[str, int] = defaultdict(int)
    for row in sorted(tables.get("reservation", []), key=lambda r: int(r["id"])):
        row = row.copy()
        date_key = str(row.get("reserve_date"))
        reservation_seq[date_key] += 1
        row["reservation_no"] = f"{date_key.replace('-', '')}{reservation_seq[date_key]:08d}"
        row["status"] = normalize_reservation_status(row.get("status"))
        reservations.append(
            trim_columns(
                row,
                [
                    "id",
                    "reservation_no",
                    "user_id",
                    "room_id",
                    "seat_id",
                    "reserve_date",
                    "start_time",
                    "end_time",
                    "status",
                    "sign_in_time",
                    "sign_out_time",
                    "cancel_reason",
                    "created_at",
                    "updated_at",
                ],
            )
        )
    out["reservation"] = reservations
    reservation_by_id = {row["id"]: row for row in reservations}
    room_by_id = {row["id"]: row for row in rooms}

    slots = []
    for row in tables.get("reservation_slot", []):
        row = row.copy()
        row["status"] = normalize_slot_status(row.get("status"))
        if row["status"] == "占用":
            slots.append(row)
    out["reservation_slot"] = slots

    checkins = []
    for row in tables.get("checkin_record", []):
        row = row.copy()
        row["checkin_method"] = normalize_checkin_method(row.get("checkin_method"))
        row["result"] = normalize_checkin_result(row.get("result"))
        checkins.append(row)
    out["checkin_record"] = checkins

    credits = []
    for row in tables.get("credit_log", []):
        row = row.copy()
        row["change_type"] = normalize_credit_type(row.get("change_type"))
        row["before_score"] = max(0, min(500, int(row.get("before_score") or 0)))
        row["after_score"] = max(0, min(500, int(row.get("after_score") or 0)))
        credits.append(row)
    out["credit_log"] = credits

    notifications = []
    for row in tables.get("notification_message", []):
        row = row.copy()
        row["type"] = normalize_notification_type(row.get("type"))
        reservation = reservation_by_id.get(row.get("related_id"))
        if row.get("title") == "预约成功" and reservation:
            room = room_by_id.get(reservation.get("room_id"), {})
            seat = seat_by_id.get(reservation.get("seat_id"), {})
            row["content"] = f"你已预约 {room.get('name', '自习室')} {seat.get('seat_no', '')}，请按时签到。"
        notifications.append(row)
    out["notification_message"] = notifications

    feedback = []
    for row in tables.get("feedback_ticket", []):
        row = row.copy()
        row["type"] = normalize_feedback_type(row.get("type"))
        row["severity"] = normalize_feedback_severity(row.get("severity"))
        row["status"] = normalize_feedback_status(row.get("status"))
        seat = seat_by_id.get(row.get("seat_id"))
        if seat and row.get("content"):
            row["content"] = re.sub(r"[A-Z0-9]+-\d{4}", str(seat["seat_no"]), str(row["content"]))
        feedback.append(row)
    out["feedback_ticket"] = feedback

    operations = []
    for row in tables.get("operation_log", []):
        row = row.copy()
        row["module"] = normalize_operation_module(row.get("module"))
        row["action"] = normalize_operation_action(row.get("action"))
        row["target_type"] = normalize_operation_target(row.get("target_type"))
        operations.append(row)
    out["operation_log"] = operations

    blacklist = []
    for row in tables.get("blacklist_record", []):
        row = row.copy()
        row["status"] = normalize_blacklist_status(row.get("status"))
        blacklist.append(row)
    out["blacklist_record"] = blacklist

    return out


TABLE_COLUMNS = OrderedDict(
    [
        ("admin_account", ["id", "account", "password_hash", "name", "role", "phone", "status", "created_at", "updated_at"]),
        ("user_account", ["id", "username", "password_hash", "role", "status", "last_login_at", "created_at", "updated_at"]),
        (
            "student_profile",
            [
                "id",
                "user_id",
                "student_no",
                "name",
                "gender",
                "college",
                "major",
                "grade",
                "phone",
                "email",
                "material_url",
                "audit_status",
                "audit_remark",
                "credit_score",
                "created_at",
                "updated_at",
            ],
        ),
        (
            "study_room",
            [
                "id",
                "room_code",
                "name",
                "room_type",
                "location",
                "floor",
                "open_time",
                "close_time",
                "status",
                "manager_id",
                "row_count",
                "col_count",
                "layout_image_url",
                "created_at",
                "updated_at",
            ],
        ),
        ("facility", ["id", "name", "created_at"]),
        ("study_room_facility", ["room_id", "facility_id"]),
        (
            "seat",
            [
                "id",
                "room_id",
                "seat_no",
                "row_no",
                "col_no",
                "is_seat",
                "cell_category",
                "seat_type",
                "has_power",
                "near_window",
                "quiet_zone",
                "hot_seat",
                "status",
                "created_at",
                "updated_at",
            ],
        ),
        (
            "announcement",
            [
                "id",
                "title",
                "content",
                "type",
                "pinned",
                "scope",
                "room_id",
                "publisher_id",
                "status",
                "published_at",
                "view_count",
                "created_at",
                "updated_at",
            ],
        ),
        (
            "reservation",
            [
                "id",
                "reservation_no",
                "user_id",
                "room_id",
                "seat_id",
                "reserve_date",
                "start_time",
                "end_time",
                "status",
                "sign_in_time",
                "sign_out_time",
                "cancel_reason",
                "created_at",
                "updated_at",
            ],
        ),
        ("reservation_slot", ["id", "reservation_id", "seat_id", "slot_start", "slot_end", "status"]),
        (
            "checkin_record",
            ["id", "reservation_id", "user_id", "admin_id", "checkin_method", "checkin_time", "checkout_time", "result", "remark"],
        ),
        ("credit_log", ["id", "user_id", "before_score", "change_value", "after_score", "change_type", "reason", "reservation_id", "created_at"]),
        ("notification_message", ["id", "user_id", "title", "content", "type", "read_flag", "related_id", "created_at", "read_at"]),
        (
            "feedback_ticket",
            [
                "id",
                "user_id",
                "reservation_id",
                "room_id",
                "seat_id",
                "type",
                "severity",
                "content",
                "status",
                "handler_id",
                "handle_result",
                "created_at",
                "handled_at",
            ],
        ),
        ("operation_log", ["id", "operator_id", "operator_name", "module", "action", "target_type", "target_id", "detail", "created_at"]),
        ("blacklist_record", ["id", "user_id", "start_time", "end_time", "reason", "status", "released_at"]),
    ]
)


def auto_increment(tables: OrderedDict[str, list[dict[str, object]]], table: str) -> int:
    ids = [int(row["id"]) for row in tables.get(table, []) if row.get("id") is not None]
    return (max(ids) + 1) if ids else 1


def schema_sql(tables: OrderedDict[str, list[dict[str, object]]]) -> str:
    ai = lambda table: auto_increment(tables, table)
    return f"""-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: study_room_reservation
-- Third-version normalized schema
-- ------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+08:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

DROP VIEW IF EXISTS `v_room_daily_usage`;
DROP TABLE IF EXISTS `study_room_facility`;
DROP TABLE IF EXISTS `feedback_ticket`;
DROP TABLE IF EXISTS `notification_message`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `blacklist_record`;
DROP TABLE IF EXISTS `credit_log`;
DROP TABLE IF EXISTS `checkin_record`;
DROP TABLE IF EXISTS `reservation_slot`;
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `seat`;
DROP TABLE IF EXISTS `facility`;
DROP TABLE IF EXISTS `study_room`;
DROP TABLE IF EXISTS `student_profile`;
DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `admin_account`;
DROP TABLE IF EXISTS `user_account`;

CREATE TABLE `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(30) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `role` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_user_role_status` (`role`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("user_account")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `admin_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `account` varchar(30) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `name` varchar(20) NOT NULL,
  `role` varchar(20) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account` (`account`),
  KEY `idx_admin_role` (`role`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("admin_account")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `student_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `student_no` varchar(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `gender` varchar(10) NOT NULL,
  `college` varchar(50) NOT NULL,
  `major` varchar(50) NOT NULL,
  `grade` varchar(10) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `material_url` varchar(255) DEFAULT NULL,
  `audit_status` varchar(20) NOT NULL,
  `audit_remark` varchar(255) DEFAULT NULL,
  `credit_score` int NOT NULL DEFAULT '300',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  UNIQUE KEY `student_no` (`student_no`),
  KEY `idx_student_audit_status` (`audit_status`),
  KEY `idx_student_college_major` (`college`,`major`),
  CONSTRAINT `fk_student_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("student_profile")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `study_room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_code` varchar(10) NOT NULL,
  `name` varchar(50) NOT NULL,
  `room_type` varchar(10) NOT NULL DEFAULT '普通',
  `location` varchar(100) NOT NULL,
  `floor` varchar(20) NOT NULL,
  `open_time` time NOT NULL,
  `close_time` time NOT NULL,
  `status` varchar(20) NOT NULL,
  `manager_id` bigint NOT NULL,
  `row_count` int NOT NULL,
  `col_count` int NOT NULL,
  `layout_image_url` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `room_code` (`room_code`),
  KEY `idx_room_manager` (`manager_id`),
  KEY `idx_room_status` (`status`),
  CONSTRAINT `fk_study_room_manager` FOREIGN KEY (`manager_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("study_room")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `facility` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("facility")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `study_room_facility` (
  `room_id` bigint NOT NULL,
  `facility_id` bigint NOT NULL,
  PRIMARY KEY (`room_id`,`facility_id`),
  KEY `idx_room_facility_facility` (`facility_id`),
  CONSTRAINT `fk_room_facility_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_room_facility_facility` FOREIGN KEY (`facility_id`) REFERENCES `facility` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `seat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `seat_no` varchar(10) NOT NULL,
  `row_no` int NOT NULL,
  `col_no` int NOT NULL,
  `is_seat` tinyint NOT NULL DEFAULT '1',
  `cell_category` varchar(20) NOT NULL,
  `seat_type` varchar(10) NOT NULL,
  `has_power` tinyint NOT NULL DEFAULT '0',
  `near_window` tinyint NOT NULL DEFAULT '0',
  `quiet_zone` tinyint NOT NULL DEFAULT '0',
  `hot_seat` tinyint NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_seat` (`room_id`,`seat_no`),
  KEY `idx_seat_room_status` (`room_id`,`status`),
  KEY `idx_seat_feature` (`room_id`,`has_power`,`near_window`,`quiet_zone`),
  CONSTRAINT `fk_seat_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT={ai("seat")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_no` varchar(16) NOT NULL,
  `user_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `reserve_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `status` varchar(20) NOT NULL,
  `sign_in_time` datetime DEFAULT NULL,
  `sign_out_time` datetime DEFAULT NULL,
  `cancel_reason` varchar(200) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reservation_no` (`reservation_no`),
  KEY `idx_res_user_date` (`user_id`,`reserve_date`),
  KEY `idx_res_room_date` (`room_id`,`reserve_date`),
  KEY `idx_res_seat_date` (`seat_id`,`reserve_date`),
  KEY `idx_res_status` (`status`),
  CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_reservation_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`),
  CONSTRAINT `fk_reservation_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("reservation")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `reservation_slot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint NOT NULL,
  `seat_id` bigint NOT NULL,
  `slot_start` datetime NOT NULL,
  `slot_end` datetime NOT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_seat_slot` (`seat_id`,`slot_start`),
  KEY `idx_slot_reservation` (`reservation_id`),
  CONSTRAINT `fk_slot_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_slot_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("reservation_slot")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `checkin_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reservation_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `admin_id` bigint NOT NULL,
  `checkin_method` varchar(20) NOT NULL,
  `checkin_time` datetime NOT NULL,
  `checkout_time` datetime DEFAULT NULL,
  `result` varchar(20) NOT NULL,
  `remark` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `reservation_id` (`reservation_id`),
  KEY `idx_checkin_admin_time` (`admin_id`,`checkin_time`),
  KEY `idx_checkin_user_time` (`user_id`,`checkin_time`),
  CONSTRAINT `fk_checkin_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_checkin_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_checkin_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("checkin_record")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `credit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `before_score` int NOT NULL,
  `change_value` int NOT NULL,
  `after_score` int NOT NULL,
  `change_type` varchar(30) NOT NULL,
  `reason` varchar(100) NOT NULL,
  `reservation_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_credit_user_time` (`user_id`,`created_at`),
  KEY `idx_credit_type` (`change_type`),
  KEY `idx_credit_reservation` (`reservation_id`),
  CONSTRAINT `fk_credit_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_credit_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("credit_log")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `blacklist_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `reason` varchar(200) NOT NULL,
  `status` varchar(20) NOT NULL,
  `released_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_blacklist_user_status` (`user_id`,`status`),
  KEY `idx_blacklist_end_status` (`end_time`,`status`),
  CONSTRAINT `fk_blacklist_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("blacklist_record")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `type` varchar(20) NOT NULL,
  `pinned` tinyint NOT NULL DEFAULT '0',
  `scope` varchar(20) NOT NULL,
  `room_id` bigint DEFAULT NULL,
  `publisher_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL,
  `published_at` datetime DEFAULT NULL,
  `view_count` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_announcement_status_time` (`status`,`published_at`),
  KEY `idx_announcement_room` (`room_id`),
  KEY `idx_announcement_pinned` (`pinned`),
  KEY `idx_announcement_publisher` (`publisher_id`),
  CONSTRAINT `fk_announcement_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`),
  CONSTRAINT `fk_announcement_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("announcement")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `notification_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` varchar(500) NOT NULL,
  `type` varchar(20) NOT NULL,
  `read_flag` tinyint NOT NULL DEFAULT '0',
  `related_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `read_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_read` (`user_id`,`read_flag`),
  KEY `idx_notification_user_time` (`user_id`,`created_at`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("notification_message")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `feedback_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `reservation_id` bigint DEFAULT NULL,
  `room_id` bigint DEFAULT NULL,
  `seat_id` bigint DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `severity` varchar(20) NOT NULL,
  `content` varchar(1000) NOT NULL,
  `status` varchar(20) NOT NULL,
  `handler_id` bigint DEFAULT NULL,
  `handle_result` varchar(1000) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `handled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_status` (`status`),
  KEY `idx_feedback_room_status` (`room_id`,`status`),
  KEY `idx_feedback_user_time` (`user_id`,`created_at`),
  KEY `idx_feedback_reservation` (`reservation_id`),
  KEY `idx_feedback_seat` (`seat_id`),
  KEY `idx_feedback_handler` (`handler_id`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_feedback_reservation` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`),
  CONSTRAINT `fk_feedback_room` FOREIGN KEY (`room_id`) REFERENCES `study_room` (`id`),
  CONSTRAINT `fk_feedback_seat` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`),
  CONSTRAINT `fk_feedback_handler` FOREIGN KEY (`handler_id`) REFERENCES `admin_account` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("feedback_ticket")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint NOT NULL,
  `operator_name` varchar(50) NOT NULL,
  `module` varchar(30) NOT NULL,
  `action` varchar(30) NOT NULL,
  `target_type` varchar(30) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `detail` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_op_log_operator` (`operator_id`,`created_at`),
  KEY `idx_op_log_module` (`module`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT={ai("operation_log")} DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE OR REPLACE VIEW `v_room_daily_usage` AS
select `sr`.`id` AS `room_id`,
       `sr`.`name` AS `room_name`,
       count(distinct case when `s`.`is_seat` = 1 then `s`.`id` end) AS `seat_count`,
       count(distinct `r`.`id`) AS `reservation_count`,
       count(distinct case when `r`.`status` in ('使用中','已完成') then `r`.`id` end) AS `used_count`,
       round(if(count(distinct case when `s`.`is_seat` = 1 then `s`.`id` end)=0,0,
         count(distinct `r`.`id`) / count(distinct case when `s`.`is_seat` = 1 then `s`.`id` end) * 100),1) AS `usage_rate`
from `study_room` `sr`
left join `seat` `s` on `s`.`room_id` = `sr`.`id`
left join `reservation` `r` on `r`.`room_id` = `sr`.`id` and `r`.`reserve_date` = curdate()
group by `sr`.`id`,`sr`.`name`;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
"""


def write_insert(table: str, columns: list[str], rows: list[dict[str, object]], chunk_size: int = 500) -> str:
    if not rows:
        return ""
    chunks: list[str] = []
    col_sql = ", ".join(f"`{col}`" for col in columns)
    for start in range(0, len(rows), chunk_size):
        part = rows[start : start + chunk_size]
        values = []
        for row in part:
            values.append("(" + ",".join(sql_value(row.get(col)) for col in columns) + ")")
        chunks.append(
            f"LOCK TABLES `{table}` WRITE;\n"
            f"/*!40000 ALTER TABLE `{table}` DISABLE KEYS */;\n"
            f"INSERT INTO `{table}` ({col_sql}) VALUES\n"
            + ",\n".join(values)
            + ";\n"
            f"/*!40000 ALTER TABLE `{table}` ENABLE KEYS */;\n"
            "UNLOCK TABLES;\n"
        )
    return "\n".join(chunks)


def data_sql(tables: OrderedDict[str, list[dict[str, object]]]) -> str:
    parts = [
        """-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: study_room_reservation
-- Third-version normalized demo data
-- ------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+08:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
"""
    ]
    for table, columns in TABLE_COLUMNS.items():
        parts.append(write_insert(table, columns, tables.get(table, [])))
    parts.append(
        """/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
"""
    )
    return "\n".join(part for part in parts if part)


def validate(tables: OrderedDict[str, list[dict[str, object]]]) -> None:
    users = {row["id"] for row in tables["user_account"]}
    admins = {row["id"] for row in tables["admin_account"]}
    rooms = {row["id"] for row in tables["study_room"]}
    seats = {row["id"] for row in tables["seat"]}
    reservations = {row["id"] for row in tables["reservation"]}
    facilities = {row["id"] for row in tables["facility"]}

    checks = [
        all(re.fullmatch(r"\d{12}", str(row["student_no"])) for row in tables["student_profile"]),
        all(re.fullmatch(r"\d{4}", str(row["grade"])) for row in tables["student_profile"]),
        all(re.fullmatch(r"\d{16}", str(row["reservation_no"])) for row in tables["reservation"]),
        all(len(str(row["room_code"])) <= 10 for row in tables["study_room"]),
        all(len(str(row["seat_no"])) <= 10 for row in tables["seat"]),
        all(0 <= int(row["credit_score"]) <= 500 for row in tables["student_profile"]),
        all(row["user_id"] in users for row in tables["student_profile"]),
        all(row["manager_id"] in admins for row in tables["study_room"]),
        all(row["room_id"] in rooms and row["facility_id"] in facilities for row in tables["study_room_facility"]),
        all(row["room_id"] in rooms for row in tables["seat"]),
        all(row["user_id"] in users and row["room_id"] in rooms and row["seat_id"] in seats for row in tables["reservation"]),
        all(row["reservation_id"] in reservations and row["seat_id"] in seats for row in tables["reservation_slot"]),
        all(row["reservation_id"] in reservations and row["user_id"] in users and row["admin_id"] in admins for row in tables["checkin_record"]),
        all(row["user_id"] in users and (row["reservation_id"] is None or row["reservation_id"] in reservations) for row in tables["credit_log"]),
        all(row["user_id"] in users for row in tables["blacklist_record"]),
        all((row["room_id"] is None or row["room_id"] in rooms) and row["publisher_id"] in admins for row in tables["announcement"]),
        all(row["user_id"] in users for row in tables["notification_message"]),
        all(
            row["user_id"] in users
            and (row["reservation_id"] is None or row["reservation_id"] in reservations)
            and (row["room_id"] is None or row["room_id"] in rooms)
            and (row["seat_id"] is None or row["seat_id"] in seats)
            and (row["handler_id"] is None or row["handler_id"] in admins)
            for row in tables["feedback_ticket"]
        ),
    ]
    if not all(checks):
        failed = [idx + 1 for idx, ok in enumerate(checks) if not ok]
        raise ValueError(f"validation failed: {failed}")


def main() -> None:
    source = FULL_SQL.read_text(encoding="utf-8")
    parsed = parse_inserts(source)
    normalized = normalize_tables(parsed)
    validate(normalized)
    schema = schema_sql(normalized)
    data = data_sql(normalized)
    SCHEMA_SQL.write_text(schema, encoding="utf-8", newline="\n")
    DATA_SQL.write_text(data, encoding="utf-8", newline="\n")
    FULL_SQL.write_text(schema + "\n" + data, encoding="utf-8", newline="\n")
    print(f"wrote {SCHEMA_SQL}")
    print(f"wrote {DATA_SQL}")
    print(f"wrote {FULL_SQL}")


if __name__ == "__main__":
    main()
