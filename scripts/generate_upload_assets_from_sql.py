from __future__ import annotations

import hashlib
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
SQL_PATH = next((ROOT / "docs").glob("06-*")) / "data.sql"
UPLOADS = ROOT / "uploads"
LAYOUT_DIR = UPLOADS / "layout"
MATERIAL_DIR = UPLOADS / "material"
FONT_REGULAR = Path("C:/Windows/Fonts/NotoSansSC-VF.ttf")
FONT_BOLD = Path("C:/Windows/Fonts/simhei.ttf")


def extract_insert_values(sql: str, table: str) -> str:
    marker = f"INSERT INTO `{table}`"
    start = sql.index(marker)
    values_start = sql.index(" VALUES", start) + len(" VALUES")
    end = sql.index(";\n", values_start)
    return sql[values_start:end].strip()


def extract_all_insert_values(sql: str, table: str) -> list[str]:
    values: list[str] = []
    marker = f"INSERT INTO `{table}`"
    start = 0
    while True:
        try:
            insert_start = sql.index(marker, start)
        except ValueError:
            break
        values_start = sql.index(" VALUES", insert_start) + len(" VALUES")
        end = sql.index(";\n", values_start)
        values.append(sql[values_start:end].strip())
        start = end + 2
    return values


def parse_sql_values(values: str) -> list[list[object]]:
    rows: list[list[object]] = []
    row: list[object] = []
    token: list[str] = []
    in_string = False
    escape = False
    active = False

    def flush() -> None:
        raw = "".join(token).strip()
        token.clear()
        if raw.upper() == "NULL":
            row.append(None)
        elif raw.startswith("'") and raw.endswith("'"):
            row.append(raw[1:-1].replace("\\'", "'").replace("\\\\", "\\"))
        elif raw == "":
            row.append("")
        else:
            try:
                row.append(int(raw))
            except ValueError:
                row.append(raw)

    for ch in values:
        if not active:
            if ch == "(":
                active = True
                row = []
                token = []
            continue
        token.append(ch)
        if in_string:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == "'":
                in_string = False
            continue
        if ch == "'":
            in_string = True
        elif ch == ",":
            token.pop()
            flush()
        elif ch == ")":
            token.pop()
            flush()
            rows.append(row)
            active = False
    return rows


def load_data() -> tuple[list[dict[str, object]], list[dict[str, object]], list[dict[str, object]]]:
    sql = SQL_PATH.read_text(encoding="utf-8")
    student_cols = [
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
    ]
    room_cols = [
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
    ]
    facility_cols = ["id", "name", "created_at"]
    room_facility_cols = ["room_id", "facility_id"]
    seat_cols = [
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
    ]
    students = [dict(zip(student_cols, row)) for row in parse_sql_values(extract_insert_values(sql, "student_profile"))]
    rooms = [dict(zip(room_cols, row)) for row in parse_sql_values(extract_insert_values(sql, "study_room"))]
    facilities = {row["id"]: row["name"] for row in [dict(zip(facility_cols, row)) for row in parse_sql_values(extract_insert_values(sql, "facility"))]}
    room_facilities: dict[int, list[str]] = {}
    for row in [dict(zip(room_facility_cols, row)) for row in parse_sql_values(extract_insert_values(sql, "study_room_facility"))]:
        room_facilities.setdefault(int(row["room_id"]), []).append(str(facilities.get(row["facility_id"], "")))
    seat_rows: list[list[object]] = []
    for values in extract_all_insert_values(sql, "seat"):
        seat_rows.extend(parse_sql_values(values))
    seats = [dict(zip(seat_cols, row)) for row in seat_rows]
    seat_count_by_room: dict[int, int] = {}
    for seat in seats:
        if int(seat["is_seat"]) == 1:
            seat_count_by_room[int(seat["room_id"])] = seat_count_by_room.get(int(seat["room_id"]), 0) + 1
    for room in rooms:
        room["facilities"] = ",".join(x for x in room_facilities.get(int(room["id"]), []) if x)
        room["seat_count"] = seat_count_by_room.get(int(room["id"]), 0)
    return students, rooms, seats


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(str(FONT_BOLD if bold else FONT_REGULAR), size=size)


def text_fit(draw: ImageDraw.ImageDraw, text: str, max_width: int, size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    selected = font(size, bold)
    while size > 12 and draw.textbbox((0, 0), text, font=selected)[2] > max_width:
        size -= 1
        selected = font(size, bold)
    return selected


def draw_round(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], fill: str, outline: str, radius: int = 14, width: int = 2) -> None:
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def generate_layout(room: dict[str, object], seats: list[dict[str, object]]) -> Path:
    rows = int(room["row_count"])
    cols = int(room["col_count"])
    layout_url = str(room["layout_image_url"])
    out = ROOT / layout_url.lstrip("/")
    out.parent.mkdir(parents=True, exist_ok=True)

    width = 1800
    header_h = 270
    legend_h = 110
    margin = 88
    gap = 14
    max_cell_w = (width - margin * 2 - gap * (cols - 1)) / cols
    cell_w = int(min(112, max_cell_w))
    cell_h = 76
    grid_w = cols * cell_w + (cols - 1) * gap
    grid_h = rows * cell_h + (rows - 1) * gap
    height = header_h + grid_h + legend_h + 80

    img = Image.new("RGB", (width, height), "#f6f8f2")
    draw = ImageDraw.Draw(img)
    for y in range(0, height, 48):
        draw.line([(0, y), (width, y)], fill="#eef2e7", width=1)

    draw.rectangle((0, 0, width, 18), fill="#2f6f5f")
    draw.text((margin, 54), str(room["name"]), fill="#17352f", font=font(42, True))
    meta = f"{room['room_code']} · {room['location']} · {room['floor']} · {room['open_time']}-{room['close_time']}"
    draw.text((margin, 120), meta, fill="#5f6f69", font=font(24))
    draw.text((margin, 166), f"设施：{room['facilities']}    座位：{room['seat_count']} / 网格：{rows}×{cols}", fill="#607069", font=font(23))
    draw.rounded_rectangle((width - 380, 52, width - margin, 190), radius=22, fill="#ffffff", outline="#d6dfd2", width=2)
    draw.text((width - 346, 82), "座位分布图", fill="#2f6f5f", font=font(30, True))
    draw.text((width - 346, 128), "编号与系统座位表一致", fill="#7b8a84", font=font(20))

    by_cell = {(int(s["row_no"]), int(s["col_no"])): s for s in seats if int(s["room_id"]) == int(room["id"])}
    start_x = (width - grid_w) // 2
    start_y = header_h
    palette = {
        "空闲": ("#ffffff", "#83b59e", "#1f4d42"),
        "维修": ("#fff2dc", "#deab57", "#7a4b00"),
        "停用": ("#eceff1", "#aab4b8", "#647078"),
    }
    feature_fill = "#e7ebe3"
    feature_outline = "#c5cec0"
    for r in range(1, rows + 1):
        for c in range(1, cols + 1):
            x = start_x + (c - 1) * (cell_w + gap)
            y = start_y + (r - 1) * (cell_h + gap)
            seat = by_cell.get((r, c))
            if not seat or int(seat["is_seat"]) != 1:
                draw_round(draw, (x, y, x + cell_w, y + cell_h), feature_fill, feature_outline, 13, 2)
                label = "通道" if not seat else str(seat["cell_category"])
                f = text_fit(draw, label, cell_w - 16, 20, True)
                bbox = draw.textbbox((0, 0), label, font=f)
                draw.text((x + (cell_w - bbox[2]) / 2, y + (cell_h - bbox[3]) / 2 - 2), label, fill="#74827b", font=f)
                continue

            fill, outline, ink = palette.get(str(seat["status"]), palette["空闲"])
            draw_round(draw, (x, y, x + cell_w, y + cell_h), fill, outline, 13, 3)
            seat_no = str(seat["seat_no"])
            f = text_fit(draw, seat_no, cell_w - 14, 24, True)
            bbox = draw.textbbox((0, 0), seat_no, font=f)
            draw.text((x + (cell_w - bbox[2]) / 2, y + 13), seat_no, fill=ink, font=f)
            flags = []
            if int(seat["has_power"]):
                flags.append("电")
            if int(seat["near_window"]):
                flags.append("窗")
            if int(seat["quiet_zone"]):
                flags.append("静")
            if int(seat["hot_seat"]):
                flags.append("热")
            if flags:
                chip = " ".join(flags)
                f2 = font(15, True)
                bbox2 = draw.textbbox((0, 0), chip, font=f2)
                draw.rounded_rectangle((x + 9, y + cell_h - 25, x + 17 + bbox2[2], y + cell_h - 6), radius=8, fill="#e9f4ef")
                draw.text((x + 13, y + cell_h - 27), chip, fill="#4c806f", font=f2)

    legend_y = start_y + grid_h + 38
    legend = [("正常", "#ffffff", "#83b59e"), ("维护/停用", "#fff2dc", "#deab57"), ("通道/设施", feature_fill, feature_outline), ("电/窗/静/热", "#e9f4ef", "#8bbda8")]
    x = margin
    for label, fill, outline in legend:
        draw.rounded_rectangle((x, legend_y, x + 42, legend_y + 32), radius=8, fill=fill, outline=outline, width=2)
        draw.text((x + 54, legend_y - 2), label, fill="#51635d", font=font(20))
        x += 220
    draw.text((width - 470, legend_y - 2), "Campus Study Room Reservation", fill="#8a9892", font=font(20))
    img.save(out, "PNG", optimize=True)
    return out


def stable_matrix(seed: str, n: int = 9) -> list[list[int]]:
    digest = hashlib.sha256(seed.encode("utf-8")).digest()
    bits = []
    for b in digest:
        bits.extend([(b >> i) & 1 for i in range(8)])
    return [[bits[(r * n + c) % len(bits)] for c in range(n)] for r in range(n)]


def generate_material(student: dict[str, object]) -> Path:
    material_url = str(student["material_url"])
    out = ROOT / material_url.lstrip("/")
    out.parent.mkdir(parents=True, exist_ok=True)
    pdfmetrics.registerFont(TTFont("NotoSC", str(FONT_REGULAR)))
    pdfmetrics.registerFont(TTFont("SimHei", str(FONT_BOLD)))

    c = canvas.Canvas(str(out), pagesize=A4)
    w, h = A4
    c.setFillColor(colors.HexColor("#f7f8f4"))
    c.rect(0, 0, w, h, fill=1, stroke=0)
    c.setFillColor(colors.HexColor("#2f6f5f"))
    c.rect(0, h - 16 * mm, w, 16 * mm, fill=1, stroke=0)
    c.setFillColor(colors.white)
    c.setFont("SimHei", 18)
    c.drawString(22 * mm, h - 10 * mm, "华南农业大学学生身份核验材料")
    c.setFont("NotoSC", 9)
    c.drawRightString(w - 22 * mm, h - 10 * mm, "Campus Study Room Reservation")

    card_x, card_y, card_w, card_h = 22 * mm, 32 * mm, w - 44 * mm, h - 70 * mm
    c.setFillColor(colors.white)
    c.roundRect(card_x, card_y, card_w, card_h, 7 * mm, fill=1, stroke=0)
    c.setStrokeColor(colors.HexColor("#d6dfd2"))
    c.setLineWidth(1.2)
    c.roundRect(card_x, card_y, card_w, card_h, 7 * mm, fill=0, stroke=1)

    c.setFillColor(colors.HexColor("#17352f"))
    c.setFont("SimHei", 22)
    c.drawString(card_x + 14 * mm, card_y + card_h - 20 * mm, str(student["name"]))
    c.setFont("NotoSC", 11)
    c.setFillColor(colors.HexColor("#607069"))
    c.drawString(card_x + 14 * mm, card_y + card_h - 29 * mm, f"学号 {student['student_no']} · {student['grade']} · {student['major']}")

    photo_x, photo_y = card_x + card_w - 52 * mm, card_y + card_h - 55 * mm
    c.setFillColor(colors.HexColor("#e9f4ef"))
    c.roundRect(photo_x, photo_y, 34 * mm, 42 * mm, 4 * mm, fill=1, stroke=0)
    c.setFillColor(colors.HexColor("#2f6f5f"))
    c.setFont("SimHei", 24)
    c.drawCentredString(photo_x + 17 * mm, photo_y + 25 * mm, str(student["name"])[:1])
    c.setFont("NotoSC", 7)
    c.drawCentredString(photo_x + 17 * mm, photo_y + 13 * mm, str(student["student_no"]))

    fields = [
        ("性别", student["gender"]),
        ("学院", student["college"]),
        ("专业", student["major"]),
        ("年级", student["grade"]),
        ("手机号", student["phone"]),
        ("邮箱", student["email"]),
        ("审核状态", student["audit_status"]),
        ("信用积分", student["credit_score"]),
        ("材料备注", student["audit_remark"] or "无"),
    ]
    x1, y = card_x + 14 * mm, card_y + card_h - 54 * mm
    label_w = 24 * mm
    line_h = 12 * mm
    for label, value in fields:
        c.setFillColor(colors.HexColor("#8a9892"))
        c.setFont("NotoSC", 9)
        c.drawString(x1, y, label)
        c.setFillColor(colors.HexColor("#1f2d2a"))
        c.setFont("NotoSC", 10.5)
        c.drawString(x1 + label_w, y, str(value))
        c.setStrokeColor(colors.HexColor("#edf1ea"))
        c.line(x1, y - 3 * mm, card_x + card_w - 14 * mm, y - 3 * mm)
        y -= line_h

    c.setFillColor(colors.HexColor("#f2f5ee"))
    c.roundRect(card_x + 14 * mm, card_y + 19 * mm, 45 * mm, 45 * mm, 3 * mm, fill=1, stroke=0)
    matrix = stable_matrix(str(student["student_no"]))
    cell = 4 * mm
    mx, my = card_x + 18 * mm, card_y + 23 * mm
    c.setFillColor(colors.HexColor("#2f6f5f"))
    for r, row in enumerate(matrix):
        for col, bit in enumerate(row):
            if bit:
                c.rect(mx + col * cell, my + (8 - r) * cell, cell * 0.82, cell * 0.82, fill=1, stroke=0)

    c.setFillColor(colors.HexColor("#607069"))
    c.setFont("NotoSC", 8)
    c.drawString(card_x + 66 * mm, card_y + 51 * mm, "用途：校园自习室预约系统注册审核")
    c.drawString(card_x + 66 * mm, card_y + 43 * mm, "说明：本材料由系统根据数据库学生档案生成，用于课程设计演示。")
    c.drawString(card_x + 66 * mm, card_y + 35 * mm, f"档案创建时间：{student['created_at']}    最近更新：{student['updated_at']}")
    c.setFillColor(colors.HexColor("#2f6f5f"))
    c.setFont("SimHei", 13)
    c.drawRightString(card_x + card_w - 16 * mm, card_y + 20 * mm, "学生身份材料")
    c.save()
    return out


def main() -> None:
    LAYOUT_DIR.mkdir(parents=True, exist_ok=True)
    MATERIAL_DIR.mkdir(parents=True, exist_ok=True)
    students, rooms, seats = load_data()
    material_paths = [generate_material(student) for student in students]
    layout_paths = [generate_layout(room, seats) for room in rooms]
    print(f"students={len(students)} materials={len(material_paths)}")
    print(f"rooms={len(rooms)} layouts={len(layout_paths)}")
    for path in layout_paths:
        print(path.relative_to(ROOT))


if __name__ == "__main__":
    main()
